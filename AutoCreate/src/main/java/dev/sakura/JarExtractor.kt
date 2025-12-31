package dev.sakura

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object JarExtractor {
    // 这些是需要保留在本地的路径（排除列表，不提取到云端）
    private val KEEP_LOCAL = listOf(
        "dev/sakura/loader/"
    )

    private val keepLocalObfuscatedClasses = mutableSetOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("Usage: java -jar AutoCreate.jar <jarFile> <mappingsDir/changeLogFile>")
            return
        }

        val jarFile = File(args[0])
        if (!jarFile.exists() || !jarFile.isFile) {
            println("错误: JAR文件不存在: ${jarFile.absolutePath}")
            return
        }

        val mappingsArg = File(args[1])
        val changeLogFile = if (mappingsArg.isDirectory) {
            File(mappingsArg, "ChangeLog.txt")
        } else {
            mappingsArg
        }

        if (!changeLogFile.exists()) {
            println("错误: ChangeLog.txt不存在: ${changeLogFile.absolutePath}")
            return
        }

        // 加载混淆映射
        loadZKMChangeLog(changeLogFile)

        extract(jarFile, changeLogFile.parentFile)
    }

    private fun loadZKMChangeLog(changeLogFile: File) {
        println("正在加载混淆日志: ${changeLogFile.name}")
        var count = 0
        changeLogFile.forEachLine { line ->
            if (line.startsWith("Class:")) {
                try {
                    val isRenamed = line.contains("->")
                    val separator = if (isRenamed) "->" else "NameNotChanged"
                    val parts = line.split(separator)
                    
                    if (parts.isNotEmpty()) {
                        val left = parts[0].trim()
                        val originalName = left.substringAfter("Class:").trim().split(" ").last()
                        
                        val obfuscatedName = if (isRenamed && parts.size > 1) {
                            parts[1].trim().split(" ").first()
                        } else {
                            originalName
                        }

                        // Check if we should keep this class local
                        val originalPath = originalName.replace('.', '/')
                        
                        var keep = false
                        for (target in KEEP_LOCAL) {
                            if (originalPath.startsWith(target) || 
                                (!target.endsWith("/") && originalPath == target)) {
                                keep = true
                                break
                            }
                        }

                        if (keep) {
                            val obfPath = obfuscatedName.replace('.', '/')
                            keepLocalObfuscatedClasses.add(obfPath)
                            count++
                        }
                    }
                } catch (e: Exception) {
                    println("解析日志行失败: $line")
                    e.printStackTrace()
                }
            }
        }
        println("已识别 $count 个需要保留在本地的混淆类 (Total tracked: ${keepLocalObfuscatedClasses.size})")
    }

    fun extract(jarFile: File, mappingsDir: File) {
        // 创建输出目录（与 JAR 文件同名，去掉.jar后缀）
        val outputBaseName = jarFile.nameWithoutExtension
        val extractDir = File(jarFile.parentFile, "${outputBaseName}_extracted")
        val classesDir = File(extractDir, "classes")

        if (extractDir.exists()) {
            extractDir.deleteRecursively()
        }
        classesDir.mkdirs()

        println("开始提取: ${jarFile.name}")
        println("输出目录: ${extractDir.absolutePath}")

        var extractedCount = 0
        var keptCount = 0

        ZipFile(jarFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryName = entry.name

                if (!entryName.endsWith(".class")) {
                    return@forEach
                }

                if (!shouldExtract(entryName)) {
                    keptCount++
                    return@forEach
                }

                val outputFile = File(classesDir, entryName)
                outputFile.parentFile.mkdirs()

                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                extractedCount++
            }
        }

        println("提取完成: $extractedCount 个类文件 (云端)")
        println("保留本地: $keptCount 个类文件")

        copyMappings(mappingsDir, extractDir)

        val jarCopy = File(extractDir, jarFile.name)
        Files.copy(jarFile.toPath(), jarCopy.toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("已复制 JAR: ${jarCopy.name}")

        removeExtractedFromJar(jarFile)

        println("\n全部完成! 输出目录: ${extractDir.absolutePath}")
    }

    private fun removeExtractedFromJar(jarFile: File) {
        println("正在从原 JAR 中移除已提取的类...")

        val tempFile = File(jarFile.parentFile, "${jarFile.name}.tmp")
        var removedCount = 0
        var keptCount = 0

        ZipFile(jarFile).use { sourceZip ->
            ZipOutputStream(FileOutputStream(tempFile)).use { targetZip ->
                sourceZip.entries().asSequence().forEach { entry ->
                    val entryName = entry.name

                    // 如果是需要提取的.class 件，跳过（不复制到新 JAR，即从本地移除）
                    if (entryName.endsWith(".class") && shouldExtract(entryName)) {
                        removedCount++
                        return@forEach
                    }

                    val newEntry = ZipEntry(entryName)
                    targetZip.putNextEntry(newEntry)
                    if (!entry.isDirectory) {
                        sourceZip.getInputStream(entry).use { input ->
                            input.copyTo(targetZip)
                        }
                    }
                    targetZip.closeEntry()
                    keptCount++
                }
            }
        }

        jarFile.delete()
        tempFile.renameTo(jarFile)

        println("已从原 JAR 移除 $removedCount 个类文件，保留 $keptCount 个条目")
    }

    private fun shouldExtract(entryName: String): Boolean {
        // 只处理dev/sakura/下的类
        if (!entryName.startsWith("dev/sakura/")) {
            return false
        }

        for (target in KEEP_LOCAL) {
            if (entryName.startsWith(target)) {
                return false
            }
            if (target.endsWith("/").not() && entryName == "$target.class") {
                return false
            }
        }

        if (entryName.endsWith(".class")) {
            val className = entryName.removeSuffix(".class")
            if (keepLocalObfuscatedClasses.contains(className)) {
                return false
            }
        }

        // dev/sakura/下的其他类都提取到云端
        return true
    }

    private fun copyMappings(mappingsDir: File, outputDir: File) {
        val logFile = File(mappingsDir, "ChangeLog.txt")
        if (logFile.exists()) {
            val dest = File(outputDir, logFile.name)
            Files.copy(logFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            println("已复制: ${logFile.name}")
        }
    }
}
