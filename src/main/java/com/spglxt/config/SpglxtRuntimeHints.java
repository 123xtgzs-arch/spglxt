package com.spglxt.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class SpglxtRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("application.yml");
        hints.resources().registerPattern("application-*.yml");
        hints.resources().registerPattern("templates/**");
        hints.resources().registerPattern("static/**");
        hints.resources().registerPattern("public/**");
        hints.resources().registerPattern("META-INF/spring.factories");
        hints.resources().registerPattern("META-INF/spring/*");

        registerType(hints, com.spglxt.entity.Video.class);
        registerType(hints, com.spglxt.entity.Type.class);
        registerType(hints, com.spglxt.entity.CollectSite.class);
        registerType(hints, com.spglxt.dto.CollectSite.class);
        registerType(hints, com.spglxt.dto.CollectTask.class);
        registerType(hints, com.spglxt.dto.CronConfig.class);
        registerType(hints, com.spglxt.common.Result.class);
        registerType(hints, com.spglxt.config.JwtProperties.class);
    }

    private void registerType(RuntimeHints hints, Class<?> type) {
        hints.reflection().registerType(type, MemberCategory.values());
    }
}
