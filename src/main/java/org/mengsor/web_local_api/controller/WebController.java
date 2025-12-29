package org.mengsor.web_local_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.configuration.until.CryptoUtil;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.CreateNewApi;
import org.mengsor.web_local_api.model.RequestLog;
import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.model.enums.TokenUnit;
import org.mengsor.web_local_api.security.oauth.util.OAuthClientUtil;
import org.mengsor.web_local_api.services.ApiConfigService;
import org.mengsor.web_local_api.services.CreateNewApiService;
import org.mengsor.web_local_api.services.RequestLogService;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/page")
@RequiredArgsConstructor
public class WebController {

    @Value("${server.port}")
    private int serverPort;
    private final CreateNewApiService service;
    private final ApiConfigService apiConfigService;
    private final RequestLogService requestLogService;
    private final SettingCacheService settingCacheService;

    @GetMapping("/home")
    public String home(Model model) {
        log.info("Home page loaded");
        model.addAttribute("activePage", "home");
        return "home";
    }

    /**
     * UI PAGE - List all APIs
     */
    @GetMapping("/create-new-api")
    public String getPage(Model model) {
        List<CreateNewApi> mockApis = service.loadAll();
        String baseUrl = "http://localhost:" + serverPort + "/skyvva.api";
        model.addAttribute("serverPort", baseUrl);
        model.addAttribute("mockApis", mockApis);
        model.addAttribute("activePage", "create-new-api");
        return "create-new-api"; // Thymeleaf template
    }

    /**
     * SAVE NEW API FROM MODAL
     */
    @PostMapping("/create-new-api/save")
    public String save(@ModelAttribute CreateNewApi api, Model model) {
        service.save(api);
        String baseUrl = "http://localhost:" + serverPort + "/skyvva.api";
        model.addAttribute("serverPort", baseUrl);
        model.addAttribute("mockApis", service.loadAll());
        model.addAttribute("message", "API created successfully!");
        return "create-new-api";
    }

    /**
     * DELETE API
     */
    @PostMapping("/create-new-api/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Mock API deleted";
    }

    /**
     * REDIRECT TO API CONFIG PAGE WHEN CLICK NAME
     */
    @GetMapping("/create-new-api/redirect")
    public String redirectToApiConfig(@RequestParam Long id, @RequestParam String protocol) {
        // Optionally, you could load the API config here and pass as parameter
        return "redirect:/page/api-config?id=" + id + "&protocol=" + protocol;
    }

    /**
     * CLEAR ALL APIs
     */
    @PostMapping("/create-new-api/clear")
    @ResponseBody
    public String clearAllCreateNewApi() {
        service.clear();
        return "All APIs cleared";
    }

    /* UI PAGE */
    @GetMapping("/api-config")
    public String getPage(@RequestParam(required = false) Long id,@RequestParam(required = false) String protocol, Model model) {
        ApiConfig config = apiConfigService.findById(id);
        if (id != null) config.setId(id);
        if (protocol != null) config.setProtocol(protocol);
        model.addAttribute("config", config);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("activePage", "api-config");
        log.info("API Config page loaded");
        return "api-config";
    }

    /* SAVE FROM UI */
    @PostMapping("/api-config/save")
    public String save(@ModelAttribute ApiConfig config,@RequestParam Long id,@RequestParam String protocol, Model model) {
        try {
            apiConfigService.save(config);

            model.addAttribute("config", config);
            model.addAttribute("serverPort", serverPort);
            model.addAttribute("toastSuccess", "API Config saved successfully!");
            log.info("API Config saved successfully!");

        } catch (IllegalArgumentException e) {
            // Validation error (JSON / XML)
            log.error("Validation error: {}", e.getMessage());
            model.addAttribute("config", config);
            model.addAttribute("serverPort", serverPort);
            model.addAttribute("toastError", e.getMessage());
        } catch (Exception e) {
            // Unexpected error
            log.error("Failed to save API Config: {}", e.getMessage());
            model.addAttribute("config", config);
            model.addAttribute("serverPort", serverPort);
            model.addAttribute("toastError", "Failed to save API Config");
        }
        model.addAttribute("activePage", "api-config");
        return "api-config";
    }

    /* CLEAR ALL CONFIGS (POSTMAN) */
    @PostMapping("/api-config/clear")
    @ResponseBody
    public String clearAllApiConfigs() {
        apiConfigService.clear();
        return "All API configs cleared";
    }

    // Show all logs and optionally select one
    @GetMapping("/request-log")
    public String viewLogs(@RequestParam(value = "id", required = false) String id, Model model) {
        List<RequestLog> logs = requestLogService.getAllLogs();
        RequestLog selectedLog = null;

        if (id != null) {
            selectedLog = requestLogService.getLogById(id);
        } else if (!logs.isEmpty()) {
            // Default: select latest log
            selectedLog = logs.get(logs.size() - 1);
        }

        model.addAttribute("logs", logs);
        model.addAttribute("activePage", "request-log");
        model.addAttribute("selectedLog", selectedLog);

        return "request-log"; // Thymeleaf template name
    }

    // Refresh logs (redirect to GET)
    @PostMapping("/request-log/refresh")
    public String refreshLogs(Model model) {
        return "redirect:/page/request-log";
    }

    // Clear all logs
    @PostMapping("/request-log/clear")
    public String clearLogs() {
        requestLogService.clearAllLogs();
        return "redirect:/page/request-log";
    }

    @GetMapping("/setting")
    public String page(Model model) {

        SettingCache cache = settingCacheService.loadDecrypted();

        model.addAttribute("activePage", "setting");
        model.addAttribute("username", cache.getUsername());
        model.addAttribute("password",
                cache.getPassword() == null || cache.getPassword().isEmpty() ? "" : "********");
        model.addAttribute("settingCache", cache);
        model.addAttribute("serverPort", serverPort);

        return "setting";
    }

    @PostMapping("/setting/save")
    public String saveSetting(@ModelAttribute SettingCache form, Model model) {
        // If securityMode is NONE, clear username/password
        if ("NONE".equals(form.getSecurityMode().toString())) {
            form.setUsername("");
            form.setPassword("");
            form.setTokenDuration(0);
            form.setClientId("");
            form.setClientSecret("");
        }
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            form.setPassword(CryptoUtil.decrypt(form.getPassword()));
        }
        settingCacheService.save(form);

        // Return values back to the page
        model.addAttribute("settingCache", form);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("username", form.getUsername());
        model.addAttribute("password", form.getPassword());
        model.addAttribute("message", "Saved successfully");

        return "setting";
    }

    @PostMapping("/client/register")
    @ResponseBody
    public SettingCache registerClient(@RequestParam String type,
                                       @RequestParam String username,
                                       @RequestParam String tokenUnit,
                                       @RequestParam Integer tokenDuration) {
        // Generate client_id / client_secret
        String clientId = OAuthClientUtil.generateClientId();
        String clientSecret = OAuthClientUtil.generateClientSecret();

        SettingCache cache = settingCacheService.load();
        cache.setUsername(username); // keep username from Basic
        cache.setClientId(clientId);
        cache.setPassword(CryptoUtil.decrypt(cache.getPassword()));
        cache.setClientSecret(clientSecret);
        cache.setTokenUnit(TokenUnit.valueOf(tokenUnit));
        cache.setTokenDuration(tokenDuration);
        cache.setSecurityMode(SecurityMode.valueOf(type));

        // Save back
        settingCacheService.save(cache);

        return cache; // return updated cache
    }
}
