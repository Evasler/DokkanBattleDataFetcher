package com.evasler;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static WebDriver chromeDriver;
    private static final  String DB_FILEPATH = System.getProperty("user.dir") + "\\assets\\db\\dokkan.db";

    public static void main(String[] args) {
        fetch_links();
    }

    private static void fetch_links() {

        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/All_Link_Skills");
        chromeDriver.findElement(By.xpath("//div[text() = 'ACCEPT']")).click();

        List<WebElement> link_names_elms =chromeDriver.findElements(By.xpath("//td/a"));
        List<WebElement> link_effects_elms =chromeDriver.findElements(By.xpath("//tr/td[3]"));

        assert link_names_elms.size() == link_effects_elms.size();

        String temp_name;

        Map<String, String> links = new HashMap<>();

        for (int i = 0; i < link_names_elms.size(); i++) {

            temp_name = link_names_elms.get(i).getText();
            if (!links.keySet().contains(temp_name)) {
                links.put(temp_name, link_effects_elms.get(i).getText());
            }
        }

        Map<String, String> sorted_links = links.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query;

            int i = 0;
            Iterator iterator = sorted_links.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry pair = (Map.Entry)iterator.next();

                query = "INSERT INTO link_skill (link_skill_id, link_skill_name, link_skill_effect) VALUES ("
                        + i + ",'" + pair.getKey().toString().replaceAll("'", "''") + "','"
                        + pair.getValue().toString().replaceAll("'", "''") + "')";
                statement.execute(query);
                i++;
            }


            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void launch_chrome(String link) {

        String chrome_driver_property = "webdriver.chrome.driver";

        if (!System.getProperties().containsKey(chrome_driver_property)) {
            Path chrome_driver_path = Paths.get(System.getProperty("user.dir") + "\\webdrivers\\chromedriver.exe");
            System.setProperty("webdriver.chrome.driver", chrome_driver_path.toString());
        }

        chromeDriver = new ChromeDriver();
        chromeDriver.get(link);

        waitForLoad(chromeDriver);
    }

    private static void kill_chrome() {

        chromeDriver.quit();

        try {
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void waitForLoad(WebDriver chromeDriver) {

        new WebDriverWait(chromeDriver, 5000).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
    }
}
