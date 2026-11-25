package com.example;

interface IShizukuBoostService {
    String getGlobalSetting(String key);
    boolean putGlobalSetting(String key, String value);
    String setGameMode(String packageName, String mode);
    String trimBackgroundApp(String packageName);
    String getShellInfo();
    void destroy();
}
