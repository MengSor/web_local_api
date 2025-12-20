package org.mengsor.web_local_api.model.yaml;

import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.CreateNewApi;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlFactory {

    private YamlFactory() {}

    public static Yaml create() {

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Representer representer = new Representer(options);

        // 🔥 IMPORTANT: remove !!class
        representer.addClassTag(CreateNewApi.class, Tag.MAP);
        representer.addClassTag(ApiConfig.class, Tag.MAP);

        return new Yaml(representer, options);
    }
}
