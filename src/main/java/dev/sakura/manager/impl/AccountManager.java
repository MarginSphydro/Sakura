package dev.sakura.manager.impl;

import dev.sakura.Sakura;
import dev.sakura.account.config.AccountFile;
import dev.sakura.account.config.EncryptedAccountFile;
import dev.sakura.account.msa.MSAAuthenticator;
import dev.sakura.account.type.MinecraftAccount;
import dev.sakura.config.ConfigManager;
import dev.sakura.mixin.accessor.IMinecraftClient;
import net.minecraft.client.session.Session;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static dev.sakura.Sakura.mc;

public final class AccountManager {
    public static final MSAAuthenticator MSA_AUTHENTICATOR = new MSAAuthenticator();
    private final List<MinecraftAccount> accounts = new LinkedList<>();

    private AccountFile configFile;
    private boolean loading = false;

    public void postInit() {
        final Path runDir = ConfigManager.CONFIG_DIR;
        if (runDir.resolve("accounts_enc.json").toFile().exists()) {
            System.out.println("Encrypted account file exists");
            configFile = new EncryptedAccountFile(runDir);
        } else {
            System.out.println("Normal account file");
            configFile = new AccountFile(runDir);
        }

        loading = true;
        configFile.load();
        loading = false;
    }

    public void register(MinecraftAccount account) {
        accounts.add(account);
        if (configFile != null && !loading) configFile.save();
    }

    public void unregister(final MinecraftAccount account) {
        accounts.remove(account);
        if (configFile != null && !loading) configFile.save();
    }

    public void setSession(final Session session) {
        ((IMinecraftClient) mc).setSession(session);
        Sakura.LOGGER.info("Set session to {} ({})", session.getUsername(), session.getUuidOrNull());
    }

    public List<MinecraftAccount> getAccounts() {
        return accounts;
    }

    public boolean isEncrypted() {
        return configFile instanceof EncryptedAccountFile;
    }
}
