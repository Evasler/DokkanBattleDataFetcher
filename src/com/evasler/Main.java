package com.evasler;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    private static WebDriver chromeDriver;
    private static final  String PROJECT_FILEPATH = System.getProperty("user.dir");
    private static final  String DB_FILEPATH = PROJECT_FILEPATH + "\\assets\\db\\dokkan.db";

    public static void main(String[] args) {

        //fetch_links();
        fetch_medals();
    }

    public static void fetch_medals() {

        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/Items:_Awakening_Medals");
        chromeDriver.findElement(By.xpath("//div[text() = 'ACCEPT']")).click();

        List<WebElement> medal_category_elms =chromeDriver.findElements(By.xpath("//h2//a[contains(@href, '/wiki/')]"));

        List<String> medal_category = new ArrayList<>();
        for (WebElement elm : medal_category_elms) {
            medal_category.add(elm.getText().replace("Sum-Up", "").trim());
        }

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query;

            WebElement temp_medal_elm;
            WebElement page_header;
            String temp_medal_name;
            String temp_image_src;
            int total_medals = 0;
            int temp_medals_count = 0;
            String save_path = "\\assets\\images\\medals\\";
            List<String> exz_medal_ranks = new ArrayList<>();
            exz_medal_ranks.add("Bronze");
            exz_medal_ranks.add("Silver");
            exz_medal_ranks.add("Gold");
            exz_medal_ranks.add("Rainbow");

            for (int i = 1; i <= medal_category.size(); i++) {
                temp_medals_count =chromeDriver.findElements(By.xpath("(//table)[" + i + "]//tr[2]//a")).size();
                for(int j = 1; j <= temp_medals_count; j++) {
                    temp_medal_elm = chromeDriver.findElement(By.xpath("((//table)[" + i + "]//tr[2]//a)[" + j +"]"));
                    click_element(temp_medal_elm);

                    page_header = chromeDriver.findElement(By.xpath("//h1[@class = 'page-header__title']"));
                    if (!page_header.getText().contains("Extreme Z-Awakening")) {
                        temp_medal_name = chromeDriver.findElement(By.xpath("(//table//center)[1]")).getText();
                        query = "INSERT INTO awakening_medal (medal_id, medal_name, medal_category) VALUES ("
                                + (total_medals + j) + ",'" + temp_medal_name.replaceAll("'", "''") + "','"
                                + medal_category.get(i-1).replaceAll("'", "''") + "')";
                        statement.execute(query);
                        System.out.println(query);

                        temp_image_src = chromeDriver.findElement(By.xpath("(//table//a[img])[1]")).getAttribute("href");
                        while(!downloadImage(temp_image_src, save_path, total_medals + j)) {
                            browser_refresh();
                        }
                    } else {
                        for (int z = 0; z < 4; z++) {
                            temp_medal_name = page_header.getText().replace("Extreme Z-Awakening Medals:", "").trim();
                            query = "INSERT INTO awakening_medal (medal_id, medal_name, medal_category) VALUES ("
                                    + (total_medals + j) + ",'" + temp_medal_name.replaceAll("'", "''") +
                                    " " + exz_medal_ranks.get(z) + "','"
                                    + medal_category.get(i-1).replaceAll("'", "''") + "')";
                            statement.execute(query);
                            System.out.println(query);

                            temp_image_src = chromeDriver.findElement(By.xpath("(//table//a[img[contains(@data-image-key,'" + exz_medal_ranks.get(z) + "') or contains(@data-image-key,'" + exz_medal_ranks.get(z).toLowerCase() + "')]])[1]")).getAttribute("href");
                            while(!downloadImage(temp_image_src, save_path, total_medals + j++)) {
                                browser_refresh();
                            }
                        }
                        j--;
                    }
                    browser_back();
                }
                total_medals += temp_medals_count;
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        waitForLoad();
    }

    static boolean downloadImage(String src, String save_path, int id) {

        boolean succeeded = true;
        try {
            System.out.println(src);
            URL imageURL = new URL(src);
            BufferedImage saveImage = ImageIO.read(imageURL);
            ImageIO.write(saveImage, "png", new File(PROJECT_FILEPATH + save_path + id + ".png"));
        } catch (IOException e) {
            succeeded = false;
            e.printStackTrace();
        }

        return succeeded;
    }

    private static void browser_refresh() {
        chromeDriver.navigate().refresh();
        waitForLoad();
    }

    private static void browser_back() {
        chromeDriver.navigate().back();
        waitForLoad();
    }

    private static void click_element(WebElement elm) {

        WebDriverWait wait = new WebDriverWait(chromeDriver, 20);
        wait.until(ExpectedConditions.elementToBeClickable(elm));

        ((JavascriptExecutor) chromeDriver).executeScript("arguments[0].click();", elm);
        waitForLoad();
    }

    private static void kill_chrome() {

        chromeDriver.quit();

        try {
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void waitForLoad() {

        chromeDriver.manage().timeouts().implicitlyWait(2000, TimeUnit.MILLISECONDS);
        new WebDriverWait(chromeDriver, 5000).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
    }
}
