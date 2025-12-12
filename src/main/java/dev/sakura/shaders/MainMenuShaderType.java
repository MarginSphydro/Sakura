package dev.sakura.shaders;

public enum MainMenuShaderType {
    MAIN_MENU("mainmenu.fsh", "主菜单"),
    //SAKURA1("mainmenu_sakura1.fsh", "樱花效果 1"),
    SAKURA2("mainmenu_sakura2.fsh", "樱花效果 2"),
    //SAKURA3("mainmenu_sakura3.fsh", "樱花效果 3"),
    SEA("mainmenu_sea.fsh", "海洋效果"),
    TOKYO("mainmenu_tokyo.fsh", "东京夜景");
    //JOURNEY("mainmenu_journey.fsh", "旅程效果"),
    //DRIVE_HOME("mainmenu_drivehome.fsh", "驾车回家"),
    //HEARTFELT("mainmenu_heartfelt.fsh", "爱心雨滴"), TODO:爱心雨滴依赖背景不然很丑
    //VIDEO("", "视频背景"); 去你妈的鸣潮

    private final String fragmentShaderPath;
    private final String displayName;

    MainMenuShaderType(String fragmentShader, String displayName) {
        this.fragmentShaderPath = "/assets/sakura/shaders/" + fragmentShader;
        this.displayName = displayName;
    }

    public String getFragmentShaderPath() {
        return fragmentShaderPath;
    }

    public String getVertexShaderPath() {
        return "/assets/sakura/shaders/mainmenu_passthrough.vsh";
    }

    public String getDisplayName() {
        return displayName;
    }

    public MainMenuShaderType next() {
        MainMenuShaderType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public MainMenuShaderType previous() {
        MainMenuShaderType[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static MainMenuShaderType fromName(String name) {
        for (MainMenuShaderType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
