package com.evasler;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    private static WebDriver firefoxDriver;
    private static final String PROJECT_FILEPATH = System.getProperty("user.dir");
    private static final String DB_FILEPATH = PROJECT_FILEPATH + "\\assets\\db\\dokkan.db";
    private static final String CARD_TABS_XPATH = "(//ul[@class = 'tabbernav'])[1]/li//a[not (@title='Transcended Ultra Rare') and not (@title='Extreme Z-Awakened')]";

    private static List<String> error_messages;

    public static void main(String[] args) {

        //fetch_links();
        //fetch_medals();
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:N", "");
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:R", "");
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:SR", "");
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:SSR", "");
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:UR", "");
        //fetch_cards("https://dbz-dokkanbattle.fandom.com/wiki/Category:LR", "The True Value of Perfect Form Cell (Perfect Form)");
    }

    public static void fetch_cards(String URL, String start_character) {

        launch_firefox(URL);
        accept_cookies();
        scroll_to_bottom();

        List<WebElement> cards_on_page = firefoxDriver.findElements(By.xpath("//div[@class='category-page__members']//a[not (img)]"));
        WebElement temp_card;
        List<WebElement> next_button;

        int start = 1;
        String xpath = "//a[contains(@class,'category-page__pagination-next')]";
        if (!start_character.equals("")) {
            while (start == 1) {
                next_button = firefoxDriver.findElements(By.xpath(xpath));
                for (int i = 0; i < cards_on_page.size(); i++) {
                    if (cards_on_page.get(i).getText().equals(start_character)) {
                        start = i + 1;
                        break;
                    }
                }
                if (start == 1) {
                    if (next_button.size() > 0) {
                        click_element(xpath);
                        cards_on_page = firefoxDriver.findElements(By.xpath("//div[@class='category-page__members']//a[not (img)]"));
                    } else {
                        System.out.println(start_character + " not found");
                        return;
                    }
                }
            }
        }

        while (true) {
            for (int i = start; i <= cards_on_page.size(); i++) {
                error_messages = new ArrayList<>();
                xpath = "(//div[@class='category-page__members']//a[not (img)])[" + i + "]";
                click_element(xpath);
                System.out.println(fetch_card_name(1));
                if (!(((fetch_card_name(1).contains("Worth") && fetch_card_name(1).contains("Zeni")) ||
                        (fetch_character_name(1).contains("Worth") && fetch_character_name(1).contains("Zeni"))) ||
                        fetch_card_name(1).equals("Mysterious Ritual") || fetch_card_name(1).equals("Light Doze") ||
                        fetch_card_name(1).equals("Big Name in the Otherworld") || fetch_card_name(1).equals("Christmas Eve Gifts") ||
                        fetch_card_name(1).equals("Overflowing Resolve") || fetch_card_name(1).equals("Message from Another World") ||
                        fetch_card_name(1).equals("Flawless Technique") || fetch_card_name(1).equals("The Last Instant Transmission") ||
                        fetch_card_name(1).equals("Legendary Super Saiyan") || fetch_card_name(1).equals("Thrilling Christmas Eve") ||
                        fetch_card_name(1).equals("1.5 Million-Zeni Price Tag") || fetch_card_name(1).equals("Charming Dream Girls") ||
                        fetch_card_name(1).equals("Heading for a Showdown") || fetch_card_name(1).equals("Fictitious Universe's Strongest") ||
                        fetch_card_name(1).equals("Glacial Prestige") || fetch_card_name(1).equals("Last-Ditch Battle") ||
                        fetch_card_name(1).equals("Roaring Hatred from Hell") || fetch_card_name(1).equals("Hidden Majin Power") ||
                        fetch_card_name(1).equals("Ultimate Malign Being") || fetch_card_name(1).equals("The Androids' Journey"))) {
                    fetch_card();
                    if (error_messages.size() > 0) {
                        update_error_file();
                    }
                }
                waitForLoad();
                browser_back();
            }
            xpath = "//a[contains(@class,'category-page__pagination-next')]";
            next_button = firefoxDriver.findElements(By.xpath(xpath));
            if (next_button.size() > 0) {
                click_element(xpath);
                start = 1;
                cards_on_page = firefoxDriver.findElements(By.xpath("//div[@class='category-page__members']//a[not (img)]"));
            } else {
                break;
            }
        }
        quit_firefoxDriver();
    }

    public static void  fetch_card() {

        scroll_to_bottom();

        List<WebElement> tabs = fetch_card_tabs();
        int tab_count = tabs.size() > 0 ? tabs.size() : 1;
        int exz_bonus = 0;

        for (int i = 0; i < tab_count; i++) {

            tabs = fetch_card_tabs();
            if (tabs.size() > 0 && i > 0) {
                System.out.println("Version " + i);
                click_element("(" + CARD_TABS_XPATH + ")[" + (i + 1) + "]");
                waitForLoad();
            }

            String card_id = fetch_card_id(i + 1);

            if (is_invincible_form(i + 1)) {
                add_card_next_form_relation(card_id, i + 1);
                add_invincible_form_card(i + 1);
            } else if (is_transformation()) {
                add_card_next_form_relation(card_id, i + 1);
                add_transformation_card(i + 1);
            } else if (is_or_can_be_exchanged()) {
                add_card_next_form_relation(card_id, i + exz_bonus + 1);
                add_exchanged_card(i + exz_bonus+ 1);
            } else {
                add_card(card_id, i + 1);
                if (acquired_from_dokkan_awakening())
                    add_card_next_form_relation(card_id, i + 1);
            }

            if (!fetch_rarity(i + 1).equals("LR")) {
                download_card_icon(card_id);
            }

            add_super_attack_and_card_super_attack_relation(card_id, i + exz_bonus + 1);
            add_active_skill_and_card_active_skill_relation(card_id, i + exz_bonus + 1);
            add_card_link_skill_relation(i + 1, card_id);
            add_card_category_relation(i + 1, card_id);

            add_card_awakening_medal_combination_relation(i + 1, "Awakening Medals", card_id);
            add_card_awakening_medal_combination_relation(i + 1, "Dokkan Awakening Medals", card_id);
            add_card_awakening_medal_combination_relation(i + 1, "Extreme-Z Awakening Medals", card_id);

            add_transformation_condition(card_id);

            if (Integer.parseInt(card_id.replace("_jp", "")) % 20 == 0) {
                add_card_hidden_potential_rank_relation(i + 1, card_id);
                add_free_to_play_cards(i + 1, card_id);
            }

            String xpath = "//li//a[@title='Extreme Z-Awakened']";
            List<WebElement> exz_awakenable_tabs = firefoxDriver.findElements(By.xpath(xpath));
            if (exz_awakenable_tabs.size() > 1) {
                xpath = "(" + xpath + ")[" + (i + 1) + "]";
                exz_awakenable_tabs = firefoxDriver.findElements(By.xpath(xpath));
            }
            if (exz_awakenable_tabs.size() > 0 && exz_awakenable_tabs.get(0).isDisplayed()) {
                exz_bonus++;
                click_element(xpath);
                card_id = fetch_card_id(i + 1);
                add_card_next_form_relation(card_id, i + 1);
                add_exz_awakened_card(card_id, i + exz_bonus + 1);
                add_super_attack_and_card_super_attack_relation(card_id, i + exz_bonus + 1);
                add_active_skill_and_card_active_skill_relation(card_id, i + exz_bonus + 1);
                add_card_link_skill_relation(i + 1, card_id);
                add_card_category_relation(i + 1, card_id);
                add_transformation_condition(card_id);
                browser_back();
            }
            tabs = fetch_card_tabs();
            tab_count = tabs.size() > 0 ? tabs.size() : 1;
        }

        for (int i = 0; i < tab_count - 1; i++) {
            browser_back();
        }
    }

    private static List<WebElement> fetch_card_tabs() {
        return firefoxDriver.findElements(By.xpath(CARD_TABS_XPATH));
    }

    private static void download_card_icon(String card_id) {
        String temp_image_src = firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')]//a[img])[1]")).getAttribute("href");
        String save_path = "\\assets\\images\\card_icons\\";
        while (!downloadImage(temp_image_src, save_path, card_id)) {
            browser_refresh();
        }
    }

    private static String fetch_card_id(int version) {
        String new_card_id = null;
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String pre_dokkan_awakening_card_id = "";
            String query = "";
            ResultSet rs;

            if (!is_japanese_version()) {
                if (is_or_can_be_exchanged() || acquired_from_exz_awakening(version) || is_invincible_form(version) || is_transformation()) {
                    String card_id = fetch_existing_card_id_by_card_name("Global", version);
                    new_card_id = Integer.toString(fetch_largest_line_card_id_used(card_id) + 1);
                } else if (acquired_from_dokkan_awakening()) {
                    String xpath = "(//table[contains(.,'How_to_obtain.png')])[" + version + "]//img[contains(@src, 'Dokkan_awaken')]/parent::a/following-sibling::a[1]";

                    click_element(xpath);
                    pre_dokkan_awakening_card_id = fetch_existing_card_id_by_card_name("Global", version);
                    browser_back();
                    new_card_id = Integer.toString(fetch_largest_line_card_id_used(pre_dokkan_awakening_card_id) + 1);
                } else {
                    query = "SELECT max(card_id) FROM card WHERE card_id NOT LIKE '%_jp'";
                    rs = statement.executeQuery(query);
                    String max_card_id = null;
                    while (rs.next()) {
                        max_card_id = rs.getString(1);
                    }

                    if (max_card_id == null) {
                        max_card_id = "9000000";
                        new_card_id = max_card_id;
                    } else {
                        int max_card_id_value = Integer.parseInt(max_card_id);
                        new_card_id = Integer.toString(max_card_id_value + 20 - (max_card_id_value % 20));
                    }
                }
            } else {
                String card_id = fetch_existing_card_id_by_card_name("Global", version);
                new_card_id = card_id + "_jp";
                /*String table_name = acquired_from_exz_awakening() ? "exz_awakened_card" : "card";
                query = "SELECT " + table_name + "_id FROM card_exz_awakened_card_relation WHERE card_id = '" + card_id + "'";
                rs = statement.executeQuery(query);
                while(rs.next()) {
                    new_card_id = rs.getString(1) + "_jp";
                }
                if (new_card_id == null) {
                    new_card_id = (fetch_largest_line_card_id_used(card_id) + 1) + "_jp";
                }*/
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        assert new_card_id != null;
        return new_card_id;
    }

    private static void add_card_next_form_relation(String new_card_id, int version) {

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "";
            if (acquired_from_exz_awakening(version)) {
                query = "UPDATE card_exz_awakened_card_relation SET exz_awakened_card_id = '" + new_card_id + "' WHERE card_id = '" +
                        fetch_largest_line_card_id_used(fetch_existing_card_id_by_card_name("All", version)) + "'";
            } else if (acquired_from_dokkan_awakening()) {
                String xpath = "(//table[contains(.,'How_to_obtain.png')])[" + version + "]//img[contains(@src, 'Dokkan_awaken')]/parent::a/following-sibling::a[1]";

                String pre_dokkan_awakening_card_id;
                if (!is_japanese_version()) {
                    click_element(xpath);
                    pre_dokkan_awakening_card_id = fetch_existing_card_id_by_card_name("Global", version);
                    browser_back();
                } else {
                    query = "SELECT card_id FROM card_dokkan_awakened_card_relation WHERE dokkan_awakened_card_id = '" + fetch_existing_card_id_by_card_name("Global", version) + "'";
                    System.out.println(query);
                    ResultSet rs = statement.executeQuery(query);
                    rs.next();
                    pre_dokkan_awakening_card_id = rs.getString(1) + "_jp";
                }

                query = "UPDATE card_dokkan_awakened_card_relation SET dokkan_awakened_card_id = '" + new_card_id + "' WHERE card_id = '" +
                        pre_dokkan_awakening_card_id + "'";
            } else if (is_or_can_be_exchanged()) {
                query = "SELECT max(card_id) FROM card WHERE card_name = '" +fetch_card_name(version) + "'";
                ResultSet rs = statement.executeQuery(query);

                query = "INSERT INTO card_exchange_card_relation (card_id, exchange_card_id) VALUES ('" +
                        rs.getString(1) + "','" + new_card_id + "')";
            } else if (is_invincible_form(version)) {
                query = "INSERT INTO card_invincible_form_card_relation (card_id, invincible_form_card_id) VALUES ('" +
                        fetch_largest_line_card_id_used(fetch_existing_card_id_by_card_name("All", version)) + "','" + new_card_id + "')";
            } else if (is_transformation()) {
                query = "INSERT INTO card_transformation_card_relation (card_id, transformation_card_id) VALUES ('" +
                        fetch_largest_line_card_id_used(fetch_existing_card_id_by_card_name("All", version)) + "','" + new_card_id + "')";
            }
            System.out.println(query);
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int fetch_largest_line_card_id_used(String card_id) {

        int largest_line_card_id_used_value = -1;

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            ResultSet rs;

            int card_id_value = Integer.parseInt(card_id.replace("_jp", ""));
            int line_base_card_id = card_id_value - (card_id_value % 20);
            int line_max_card_id = line_base_card_id + 19;
            String query = "CREATE TABLE `TempTable` (`card_id` TEXT);";
            statement.execute(query);
            query = "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(card_id) FROM card WHERE card_id >= " + line_base_card_id + " AND card_id <= " + line_max_card_id + ";";
            statement.execute(query);
            query = "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(exz_awakened_card_id) FROM exz_awakened_card WHERE exz_awakened_card_id >= " + line_base_card_id + " AND exz_awakened_card_id <= " + line_max_card_id + ";";
            statement.execute(query);
            query = "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(invincible_form_card_id) FROM invincible_form_card WHERE invincible_form_card_id >= " + line_base_card_id + " AND invincible_form_card_id <= " + line_max_card_id + ";";
            statement.execute(query);
            query = "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(transformation_card_id) FROM transformation_card WHERE transformation_card_id >= " + line_base_card_id + " AND transformation_card_id <= " + line_max_card_id + ";";
            statement.execute(query);
            query = "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(exchange_card_id) FROM exchange_card WHERE exchange_card_id >= " + line_base_card_id + " AND exchange_card_id <= " + line_max_card_id + ";";
            statement.execute(query);
            query = "SELECT max(card_id) FROM TempTable;";
            rs = statement.executeQuery(query);
            while (rs.next()) {
                largest_line_card_id_used_value = Integer.parseInt(rs.getString(1).replace("_jp", ""));
            }
            query = "DROP TABLE TempTable;";
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return largest_line_card_id_used_value;
    }

    private static String fetch_existing_card_id_by_card_name(String game_version, int version) {

        String card_id = null;
        String card_name = fetch_card_name(version);

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String amendment = "";
            if (game_version.equals("Jp")) {
                amendment = " LIKE '%_jp'";
            } else if (game_version.equals("Global")) {
                amendment = " NOT LIKE '%_jp'";
            }
            String query = "SELECT min(card_id) FROM card WHERE card_name = '" + formatForSQLite(card_name) + "' AND card_id" + amendment;
            System.out.println(query);
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                card_id = rs.getString(1);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return card_id;
    }

    private static boolean acquired_from_dokkan_awakening() {
        List<WebElement> dokkan_awaken_logo = firefoxDriver.findElements(By.xpath("(//img[contains(@src, 'How_to_obtain')])[last()]//ancestor::tr[1]/following-sibling::tr//img[contains(@src, 'Dokkan_awaken')]"));
        return dokkan_awaken_logo.size() > 0;
    }

    private static boolean acquired_from_exz_awakening(int version) {

        if (firefoxDriver.findElements(By.xpath("//li//a[@title='Extreme Z-Awakened']")).size() == 1) {
            version = 1;
        }

        List<WebElement> exz_awakening_button = firefoxDriver.findElements(By.xpath("(//div[@title = 'Transcended Ultra Rare'])["  + version + "]/preceding-sibling::ul//a[@title = 'Extreme Z-Awakened']/parent::li"));
        return exz_awakening_button.size() > 0 && exz_awakening_button.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean is_invincible_form(int version) {
        List<String> stats = fetch_max_stats(version);
        return stats.get(0).equals("∞") && stats.get(2).equals("∞");
    }

    private static boolean is_transformation() {
        List<WebElement> transformation_skill = firefoxDriver.findElements(By.xpath("//table[descendant::img[contains(@src, 'Special_Skill')]]//a[@title = 'Transformation']"));
        List<WebElement> form_tabs = firefoxDriver.findElements(By.xpath("(//ul[@class = 'tabbernav'])[1]//li"));

        return transformation_skill.size() > 0 && form_tabs.size() > 0 && !form_tabs.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean is_or_can_be_exchanged() {
        List<WebElement> form_tabs = firefoxDriver.findElements(By.xpath("(//ul[@class = 'tabbernav'])[1]//li"));
        String active_skill_name = fetch_skill_info("Active Skill Name", 1);
        return active_skill_name != null && active_skill_name.equals("Exchange") && form_tabs.size() > 0 && !form_tabs.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean is_japanese_version() {
        List<WebElement> version = firefoxDriver.findElements(By.xpath("//a[@title = 'Japan']/parent::li"));
        return version.size() > 0 && version.get(0).getAttribute("class").equals("tabberactive");
    }

    private static String fetch_card_name(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }

        String text = firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')])[" + version + "]//tr//center//b[br]")).getText();
        return text.substring(0, text.indexOf("\n")).trim();
    }

    private static String fetch_character_name(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }
        String text = firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')])[" + version + "]//tr//center//b[br]")).getText();
        return text.substring(text.indexOf("\n") + 1).trim();
    }

    private static String fetch_rarity(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }
        return firefoxDriver.findElement(By.xpath("((//table[contains(.,'Max Lv')])[" + version + "]//tr[3]/td[3]//img)[1]")).getAttribute("alt").replace(" icon", "").replace("_logo_apng.png", "");
    }

    private static String fetch_type(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }
        return firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')])[" + version + "]//tr[3]/td[4]//a[last()]")).getAttribute("title").replace("Category:", "");
    }

    private static String fetch_cost(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }
        String text = firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')])[" + version + "]//tr[3]/td[5]")).getText();
        return text;
    }

    private static int fetch_max_sa_lvl(int version) {
        List<WebElement> tabs = fetch_card_tabs();
        if (tabs.size() > 1) {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }
        String text = firefoxDriver.findElement(By.xpath("(//table[contains(.,'Max Lv')])[" + version + "]//tr[3]/td[2]")).getText();
        text = text.substring(text.indexOf("/") + 1);
        return Integer.parseInt(text);
    }

    private static String fetch_skill_info(String skill_type, int version) {
        String png = null;
        String amendment;

        if (skill_type.contains("Leader Skill")) {
            png = "Leader_Skill";
        } else if (skill_type.contains("Super Attack")) {
            png = "Super_atk";
        } else if (skill_type.contains("Passive Skill")) {
            png = "Passive_skill";
        } else if (skill_type.contains("Active Skill")) {
            png = "Active_skill";
        }

        if (skill_type.contains("Name")) {
            amendment = "//ancestor::td)[last()]/following-sibling::td[1]//strong";
        } else {
            amendment = "//ancestor::tr)[last()]/following-sibling::tr[1]//td";
        }

        String xpath = "((//img[contains(@src,'" + png + ".png')])[" + version + "]" + amendment;
        List<WebElement> element = firefoxDriver.findElements(By.xpath(xpath));
        return element.size() > 0 ? clean_text(element.get(0).getAttribute("innerHTML"), png, amendment) : null;
    }

    private static String clean_text(String text, String png, String amendment) {
        text = clean_from_icons(text, png, amendment);
        text = clean_from_links(text, png, amendment);
        text = text.replaceAll("\n", "");
        text = clean_from_tags(text, png);
        text = text.replaceAll("\\[\\d]", "");
        text = text.replaceAll("&amp;", "&");

        return text;
    }

    public static String clean_from_tags(String text, String png) {

        String br_replacement;
        if (png.equals("Super_atk")) {
            br_replacement = "\n";
        } else {
            br_replacement = " ";
        }

        text = text.replaceAll("<br>", br_replacement);
        text = text.replaceAll("<[^<>]+>", "");
        text = text.replaceAll("</[^<>]+>", "");
        return text;
    }

    public static String clean_from_links(String text, String png, String amendment) {
        List<WebElement> links = firefoxDriver.findElements(By.xpath("((//img[contains(@src,'" + png + ".png')])[last()]" + amendment + "//a[not (img)]"));
        for (WebElement link : links) {
            text = text.replace(link.getAttribute("outerHTML"), link.getText());
        }
        return text;
    }

    public static String clean_from_icons(String text, String png, String amendment) {
        List<WebElement> icons = firefoxDriver.findElements(By.xpath("((//img[contains(@src,'" + png + ".png')])[last()]" + amendment + "//img"));
        for (WebElement icon : icons) {
            String type = icon.getAttribute("alt").replace("icon", "").trim();

            if (type.length() == 4) {
                switch (type) {
                    case "SAGL":
                        type = "Super AGL";
                        break;
                    case "SSTR":
                        type = "Super STR";
                        break;
                    case "SPHY":
                        type = "Super PHY";
                        break;
                    case "SINT":
                        type = "Super INT";
                        break;
                    case "STEQ":
                        type = "Super TEQ";
                        break;
                    case "EAGL":
                        type = "Extreme AGL";
                        break;
                    case "ESTR":
                        type = "Extreme STR";
                        break;
                    case "EPHY":
                        type = "Extreme PHY";
                        break;
                    case "EINT":
                        type = "Extreme INT";
                        break;
                    default:
                        type = "Extreme TEQ";
                        break;
                }
            }
            text = text.replace(icon.findElement(By.xpath(".//parent::a")).getAttribute("outerHTML"), type);
        }
        return text;
    }

    private static List<String> fetch_max_stats(int version) {
        return fetch_base_stats(version, 2);
    }

    private static List<String> fetch_max_rainbow_stats(int version) {
        return fetch_base_stats(version, 4);
    }

    private static List<String> fetch_base_stats(int version, int column) {
        List<WebElement> elms = firefoxDriver.findElements(By.xpath("(//table[contains(.,'Stats.png')])[" + version + "]//td[not (contains(.,'png'))][" + column + "]"));
        List<String> stats = new ArrayList<>();
        for (WebElement elm : elms) {
            stats.add(elm.getText());
        }
        return stats;
    }

    private static List<String> fetch_exz_stats(int version) {
        List<WebElement> elms = firefoxDriver.findElements(By.xpath("(//img[contains(@src,'Extreme_z_awaken')])[" + version + "]//ancestor::table//tr[position() > 3]//td[5]"));
        List<String> stats = new ArrayList<>();
        for (WebElement elm : elms) {
            stats.add(!elm.getText().equals("∞") ? elm.getText() : "9000001");
        }
        return stats;
    }

    private static Integer fetch_twelve_ki_multiplier(int version) {
        List<WebElement> elm = firefoxDriver.findElements(By.xpath("(//table[contains(.,'Additional Information.png')])[" + version + "]//p[contains(.,'Multiplier')]"));

        if (elm.size() < 1) {
            error_messages.add("Unavailable 12 Ki Multiplier: " + fetch_card_name(version));
            return 0;
        }

        String text = elm.get(0).getText();
        Pattern ki_pattern = Pattern.compile("12 Ki[0-9]{3}%");
        Matcher matcher = ki_pattern.matcher(text);
        if (matcher.find()) {
            text = matcher.group();
        }
        Pattern multiplier_pattern = Pattern.compile("[0-9]{3}%");
        matcher = multiplier_pattern.matcher(text);
        String multiplier = null;
        if (matcher.find()) {
            multiplier = matcher.group().replaceAll("%", "");
        }
        return Integer.parseInt(multiplier);
    }

    private static String fetch_hidden_potential_rank(int version) {
        List<String> max_stats = fetch_max_stats(version);
        List<String> max_rainbow_stats = fetch_max_rainbow_stats(version);

        int hp_boost = Integer.parseInt(max_rainbow_stats.get(0)) - Integer.parseInt(max_stats.get(0));
        int atk_boost = Integer.parseInt(max_rainbow_stats.get(1)) - Integer.parseInt(max_stats.get(1));
        int def_boost = Integer.parseInt(max_rainbow_stats.get(2)) - Integer.parseInt(max_stats.get(2));

        String type = fetch_type(version);
        type = type.substring(type.indexOf(" ") + 1);

        String hidden_potential_rank = null;
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query;

            if (hp_boost == 4000 && atk_boost == 4000 && def_boost == 4000) {

                query = "SELECT rank_b_special_id FROM hidden_potential_rank_b_special WHERE event = '" + fetch_drop_event().replaceAll("'", "''") + "'";
            } else {
                query = "SELECT hidden_potential_rank FROM hidden_potential WHERE type = '" + type +
                        "' AND hp_boost = " + hp_boost + " AND atk_boost = " + atk_boost + " AND def_boost = " + def_boost;
            }
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                hidden_potential_rank = rs.getString(1);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hidden_potential_rank;
    }

    private static String fetch_drop_event() {
        return firefoxDriver.findElement(By.xpath("//img[contains(@src, 'How_to_obtain')]//ancestor::tr/following-sibling::tr//a")).getAttribute("title");
    }

    private static String fetch_green_ki() {
        WebElement temp_img = firefoxDriver.findElement(By.xpath("(//img[contains(@src, 'Ki_meter')]//ancestor::td/following-sibling::td//img)[1]"));
        String save_path = "\\assets\\images\\ki_meters\\";
        String save_name = temp_img.getAttribute("data-image-key").replace(".png", "");
        while (!downloadImage(temp_img.getAttribute("src"), save_path, save_name)) {
            browser_refresh();
        }
        return save_name;
    }

    private static void add_card(String card_id, int version) {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "INSERT INTO card (card_id,card_name,character_name,rarity,type,cost,max_sa_level,leader_skill,passive_skill_name," +
                    "passive_skill,max_hp,max_atk,max_def,green_ki,twelve_ki_multiplier) VALUES ('" + card_id + "','" +
                    formatForSQLite(fetch_card_name(version)) + "','" + formatForSQLite(fetch_character_name(version)) + "','" + fetch_rarity(version) + "','" + fetch_type(version) + "','" +
                    fetch_cost(version) + "'," + fetch_max_sa_lvl(version) + ",'" + formatForSQLite(Objects.requireNonNull(fetch_skill_info("Leader Skill", version))) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill Name", version))) + "','" + formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill", version))) +
                    "'," + fetch_max_stats(version).get(0) + "," + fetch_max_stats(version).get(1) + "," + fetch_max_stats(version).get(2) + ",'" +
                    fetch_green_ki() + "'," + fetch_twelve_ki_multiplier(version) + ")";
            System.out.println(query);
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_exz_awakened_card(String card_id, int index) {
        Connection connection;
        List<WebElement> tabs = fetch_card_tabs();
        int version = tabs.size() == 0 ? 1 : 0;

        List<WebElement> exz_tabs = firefoxDriver.findElements(By.xpath("//li//a[@title='Extreme Z-Awakened']"));
        if (exz_tabs.size() == 1) {
            version = 1;
        } else {
            for (int i = 1; i <= tabs.size(); i++) {
                if (tabs.get(i - 1).findElement(By.xpath("./parent::li")).getAttribute("class").equals("tabberactive")) {
                    version = i;
                    break;
                }
            }
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "INSERT INTO exz_awakened_card (exz_awakened_card_id,rarity,max_sa_level,leader_skill," +
                    "passive_skill,max_hp,max_atk,max_def) VALUES ('" + card_id + "','exz_ur',15,'" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Leader Skill", index))) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill", index))) +
                    "'," + fetch_exz_stats(version).get(0) + "," + fetch_exz_stats(version).get(1) + "," + fetch_exz_stats(version).get(2) + ")";
            System.out.println(query);
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_invincible_form_card(int version) {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "INSERT INTO invincible_form_card (invincible_form_card_id,cost,passive_skill_name,passive_skill,base_hp,base_atk,base_def) VALUES ('" + fetch_card_id(version) + "','" +
                    fetch_cost(version) + "','" + formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill Name", version))) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill", version))) + "','" + fetch_max_stats(version).get(0) + "'," +
                    fetch_max_stats(version).get(1) + ",'" + fetch_max_stats(version).get(2) + "')";
            System.out.println(query);
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_transformation_card(int version) {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "INSERT INTO transformation_card (transformation_card_id,character_name,passive_skill_name,passive_skill) VALUES ('" + fetch_card_id(version) + "','" +
                    formatForSQLite(fetch_character_name(version)) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill Name", version))) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill", version))) + "')";
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_exchanged_card(int version) {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();

            String query = "INSERT INTO exchange_card (exchange_card_id,character_name,passive_skill_name,passive_skill) VALUES ('" + fetch_card_id(version) + "','" +
                    formatForSQLite(fetch_character_name(version)) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill Name", version))) + "','" +
                    formatForSQLite(Objects.requireNonNull(fetch_skill_info("Passive Skill", version))) + "')";
            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_super_attack_and_card_super_attack_relation(String card_id, int version) {

        List<String> super_attack_name_contents = new ArrayList<>(Arrays.asList(Objects.requireNonNull(fetch_skill_info("Super Attack Name", version)).split("\n")));
        List<String> super_attack_names = new ArrayList<>();
        List<String> super_attack_types = new ArrayList<>();
        List<String> super_attack_launch_condition = new ArrayList<>();
        String ki;
        for (String content : super_attack_name_contents) {
            ki = "";
            content = content.trim();
            if (fetch_ki_meters(version).size() > 1 || (content.contains("(") && !content.matches("[^()]+(\\(Extreme\\)\\s)?\\([0-9]{1,2}-?[0-9]{1,2}\\+?\\sKi\\)$") &&
                    !content.matches("[^()]+?(\\(Extreme\\))?$"))) {
                if (acquired_from_exz_awakening(version))
                    version = 1;
                error_messages.add("Irregular SA: " + fetch_card_name(version));
                super_attack_names.clear();
                break;
            } else {
                if (content.contains("Energy attack bubble")) {
                    content = content.replace("Energy attack bubble", "").trim();
                    super_attack_types.add("Ki Blast");
                } else {
                    super_attack_types.add("Melee");
                }

                if (content.matches("[^()]+?(\\(Extreme\\))?$")) {  //contains only super attack name
                    if (hasPremierSuperAttack()) {
                        if (acquired_from_exz_awakening(version))
                            version = 1;
                        error_messages.add("Premier SA: " + fetch_card_name(version));
                        super_attack_names.clear();
                        break;
                    } else {
                        ki = "12";
                    }
                } else if (content.matches("[^()]+(\\(Extreme\\)\\s)?\\([0-9]{1,2}\\sKi\\)$")) {   //contains super attack name and single ki
                    ki = content.substring(content.replace("(Extreme)", "").indexOf("(") + 1, content.indexOf(" Ki)"));
                    content = content.replaceAll("\\([0-9]{1,2}\\sKi\\)$", "").trim();
                } else if (content.matches("[^()]+(\\(Extreme\\)\\s)?\\([0-9]{1,2}-[0-9]{1,2}\\sKi\\)$")) {  //contains super attack name and ki range
                    ki = content.substring(content.replace("(Extreme)", "").indexOf("(") + 1);
                    ki = ki.substring(0, ki.indexOf("-"));
                    content = content.replaceAll("\\([0-9]{1,2}-[0-9]{1,2}\\sKi\\)$", "").trim();
                } else if (content.matches("[^()]+(\\(Extreme\\)\\s)?\\([0-9]{1,2}\\+\\sKi\\)$")) {  //contains super attack name and ki threshold
                    ki = content.substring(content.replace("(Extreme)", "").indexOf("(") + 1);
                    ki = ki.substring(0, ki.indexOf("+"));
                    content = content.replaceAll("\\([0-9]{1,2}\\+\\sKi\\)$", "").trim();
                }
                super_attack_names.add(content);
                super_attack_launch_condition.add("ki:" + ki);
            }
        }

        if (super_attack_names.size() > 0) {
            List<String> super_attack_effects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(fetch_skill_info("Super Attack", version)).split("\n")));
            Connection connection;
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
                Statement statement = connection.createStatement();
                String query;
                query = "SELECT count(super_attack_id) FROM super_attack";
                ResultSet rs = statement.executeQuery(query);
                int super_attacks_total = rs.getInt(1);

                for (int i = 0; i < super_attack_names.size(); i++) {
                    query = "INSERT INTO card_super_attack_relation (card_id, super_attack_id) " +
                            "VALUES ('" + card_id + "'," + super_attacks_total + ")";
                    statement.execute(query);
                    query = "INSERT INTO super_attack (super_attack_id,super_attack_name,super_attack_launch_condition,super_attack_type,super_attack_effect)" +
                            "VALUES (" + super_attacks_total++ + ",'" + formatForSQLite(super_attack_names.get(i)) + "','" + super_attack_launch_condition.get(i) + "','" + super_attack_types.get(i) + "','" + formatForSQLite(super_attack_effects.get(i)).replaceAll("►", "").trim() + "')";
                    statement.execute(query);
                }
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void add_card_hidden_potential_rank_relation(int version, String card_id) {
        String rarity = fetch_rarity(version);
        if (rarity.equals("SSR") || rarity.equals("UR") || rarity.equals("LR")) {
            Connection connection;
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
                Statement statement = connection.createStatement();
                String query = "INSERT INTO card_hidden_potential_rank_relation (card_id,hidden_potential_rank) " +
                        "VALUES ('" + card_id + "','" + fetch_hidden_potential_rank(version) + "')";
                statement.execute(query);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void add_active_skill_and_card_active_skill_relation(String card_id, int version) {
        String active_skill_name = fetch_skill_info("Active Skill Name", version);
        if (active_skill_name != null) {
            Connection connection;
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
                Statement statement = connection.createStatement();
                String query = "SELECT count(active_skill_id) FROM active_skill";
                ResultSet rs = statement.executeQuery(query);

                String active_skill_type = "Melee";
                if (active_skill_name.contains("Energy attack bubble")) {
                    active_skill_name = active_skill_name.replace("Energy attack bubble", "").trim();
                    active_skill_type = "Ki Blast";
                }

                int active_skills_total = rs.getInt(1);
                query = "INSERT INTO active_skill (active_skill_id,active_skill_name,active_skill_type,active_skill_effect) " +
                        "VALUES (" + active_skills_total + ",'" + active_skill_name + "','" + active_skill_type +
                        "','" + formatForSQLite(Objects.requireNonNull(fetch_skill_info("Active Skill", version))) + "')";
                statement.execute(query);
                query = "INSERT INTO card_active_skill_relation (card_id,active_skill_id) " +
                        "VALUES ('" + card_id + "'," + active_skills_total + ")";
                statement.execute(query);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void add_card_link_skill_relation(int version, String card_id) {
        add_category_or_link_relation(version, "Link Skill", card_id);
    }

    private static void add_card_category_relation(int version, String card_id) {
        add_category_or_link_relation(version, "Category", card_id);
    }

    private static void add_category_or_link_relation(int version, String type, String card_id) {

        String icon_name;
        String table_name;
        switch (type) {
            case "Link Skill":
                icon_name = "Link_skill";
                table_name = "link_skill";
                break;
            default:    // Category
                icon_name = "Category.png";
                table_name = "category";
                break;
        }

        List<WebElement> elms = firefoxDriver.findElements(By.xpath("(//img[contains(@src,'" + icon_name + "')])[" + version + "]//ancestor::tr[1]//following-sibling::tr[1]//a"));
        if (elms.size() == 0)
            error_messages.add("No " + type + ": " + fetch_card_name(version));

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query;
            ResultSet rs;
            int id;
            for (WebElement elm : elms) {
                query = "SELECT " + table_name + "_id FROM " + table_name + " WHERE " + table_name + "_name = '" + formatForSQLite(elm.getText()) + "';";
                rs = statement.executeQuery(query);
                if (rs.next()) {
                    id = rs.getInt(1);
                    query = "INSERT INTO card_" + table_name + "_relation(card_id," + table_name + "_id) VALUES('" + card_id + "'," + id + ")";
                    statement.execute(query);
                } else {
                    error_messages.add("Invalid " + type + " (" + elm.getText() + "): " + fetch_card_name(version));
                }
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_card_awakening_medal_combination_relation(int version, String medal_type, String card_id) {
        String rarity = fetch_rarity(version);
        List<WebElement> dokkan_awakening_not_under_acquired_table = firefoxDriver.findElements(By.xpath("(//img[contains(@src,'Dokkan_awaken_logo')][not (ancestor::tr/preceding-sibling::tr//img[contains(@src,'How_to_obtain')])])[" + version + "]"));
        List<WebElement> exz_awakening_for_base_form = firefoxDriver.findElements(By.xpath("//a[@title='Transcended Ultra Rare']/parent::li[@class='tabberactive']//ancestor::*[last()]//img[contains(@src,'Extreme_z_awaken')]"));

        if ((medal_type.equals("Awakening Medals") && (rarity.equals("N") || rarity.equals("R") || rarity.equals("SR") || rarity.equals("SSR"))) ||
                (medal_type.equals("Dokkan Awakening Medals") && dokkan_awakening_not_under_acquired_table.size() > 0) ||
                (medal_type.equals("Extreme-Z Awakening Medals") && exz_awakening_for_base_form.size() > 0)) {
            add_medal_combination_and_card_relation(version, medal_type, card_id);
        }
    }

    private static void add_medal_combination_and_card_relation(int version, String medal_type, String card_id) {

        String xpath;
        //Awakening Medals OR Dokkan Awakening Medals
        if ("Extreme-Z Awakening Medals".equals(medal_type)) {
            xpath = "(//img[contains(@src,'Extreme_z_awaken')]//ancestor::tr/following-sibling::tr[1]//a)[position() = 1 or position() = 4 or position() = 6 or position() = 7]";
        } else {
            xpath = "(//img[contains(@src,'" + (medal_type.equals("Awakening Medals") ? "Awaken" : "Dokkan_awaken_logo") + "') and not (ancestor::table[descendant::img[contains(@src,'How_to_obtain')]])])[" + version + "]//ancestor::tr/following-sibling::tr[1]//a[img[not(contains(@src,'No_medals'))]]";
        }

        List<WebElement> medal_elms = firefoxDriver.findElements(By.xpath(xpath));

        if (medal_elms.size() == 0) {
            error_messages.add("Missing " + medal_type + ": " + fetch_card_name(version));
            return;
        }

        List<Integer> exz_medal_count = new ArrayList<>();
        List<String> exz_medal_ranks = new ArrayList<>();
        List<WebElement> medal_count_elms = new ArrayList<>();
        if (!medal_type.equals("Extreme-Z Awakening Medals")) {
            medal_count_elms = firefoxDriver.findElements(By.xpath("(//img[contains(@src,'" + (medal_type.equals("Awakening Medals") ? "Awaken" : "Dokkan_awaken_logo") + "') and not (ancestor::table[descendant::img[contains(@src,'How_to_obtain')]])])[" + version + "]//ancestor::tr/following-sibling::tr[1]//b"));
        } else {
            exz_medal_count.add(15);
            exz_medal_count.add(40);
            exz_medal_count.add(30);
            exz_medal_count.add(30);
            exz_medal_ranks.add("Bronze");
            exz_medal_ranks.add("Silver");
            exz_medal_ranks.add("Gold");
            exz_medal_ranks.add("Rainbow");
        }

        if (!medal_type.equals("Extreme-Z Awakening Medals") && (medal_elms.size() != medal_count_elms.size())) {
            error_messages.add("Unequal medals and medal_counts for " + medal_type + ": " + fetch_card_name(version));
            return;
        }
        List<String> medal_names = new ArrayList<>();
        Map<String, Integer> medal_count = new HashMap<>();
        Connection connection;
        for (int i = 0; i < medal_elms.size(); i++) {
            String medal_name = medal_elms.get(i).getAttribute("title").replace("Extreme Z-Awakening Medals:", "").replace("Awakening Medals: ", "").trim();
            if (medal_type.equals("Extreme-Z Awakening Medals")) {
                if (medal_name.contains("#")) {
                    medal_name = medal_name.substring(0, medal_name.indexOf("#")).trim();
                }
                medal_name = medal_name + " " + exz_medal_ranks.get(i);
            }
            medal_names.add(medal_name);

            int medal_count_value = medal_type.equals("Extreme-Z Awakening Medals") ? exz_medal_count.get(i) :
                    Integer.parseInt(medal_count_elms.get(i).getText().replace("x", "").trim());
            medal_count.put(medal_names.get(i), medal_count_value);
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            StringBuilder query = new StringBuilder();
            query.append("SELECT medal_id, medal_name FROM awakening_medal WHERE");
            for (int i = 0; i < medal_names.size(); i++) {
                query.append(" medal_name = '").append(formatForSQLite(medal_names.get(i))).append("'");
                if (i < medal_names.size() - 1) {
                    query.append(" or");
                }
            }
            query.append(" ORDER BY medal_id");
            ResultSet medal_ids_rs = statement.executeQuery(query.toString());

            if (medal_ids_rs.isClosed()) {
                error_messages.add("Missing " + medal_type + ": " + fetch_card_name(version));
                return;
            }

            Statement statement2 = connection.createStatement();
            Statement statement3 = connection.createStatement();
            ResultSet max_combination_id_rs;
            ResultSet previous_tier_id_rs;
            int max_combination_id;
            int tier = 1;
            String previous_tier_id_condition = "";
            String previous_tier_id_field = "";
            StringBuilder previous_tier_id_value_part = new StringBuilder();
            while (medal_ids_rs.next()) {
                query = new StringBuilder();
                query.append("SELECT max(tier_").append(tier).append("_combination_id) FROM tier_").append(tier).append("_awakening_combination");
                max_combination_id_rs = statement2.executeQuery(query.toString());
                if (max_combination_id_rs.getInt(1) != 0) {
                    max_combination_id = max_combination_id_rs.getInt(1);
                } else {
                    max_combination_id = tier * 1000 - 1;
                }
                if (tier > 1) {
                    previous_tier_id_condition = " AND tier_" + (tier - 1) + "_combination_id = " + previous_tier_id_value_part;
                    previous_tier_id_field = " tier_" + (tier - 1) + "_combination_id,";
                    previous_tier_id_value_part.append(", ");
                }
                query = new StringBuilder();
                query.append("INSERT INTO tier_").append(tier).append("_awakening_combination (tier_").append(tier)
                        .append("_combination_id,").append(previous_tier_id_field).append(" medal_").append(tier).append("_id, medal_")
                        .append(tier).append("_count) ").append("SELECT ").append(++max_combination_id).append(", ").append(previous_tier_id_value_part)
                        .append(medal_ids_rs.getInt(1)).append(", ").append(medal_count.get(medal_ids_rs.getString(2)))
                        .append(" WHERE NOT EXISTS (SELECT 1 FROM tier_").append(tier).append("_awakening_combination WHERE medal_")
                        .append(tier).append("_id = ").append(medal_ids_rs.getInt(1)).append(" AND medal_").append(tier).append("_count = ").append(medal_count.get(medal_ids_rs.getString(2))).append(previous_tier_id_condition).append(")");
                System.out.println(query.toString());
                statement2.execute(query.toString());

                query = new StringBuilder();
                query.append("SELECT tier_").append(tier).append("_combination_id ").append(" FROM tier_").append(tier).append("_awakening_combination WHERE medal_")
                        .append(tier).append("_id = ").append(medal_ids_rs.getInt(1)).append(previous_tier_id_condition);
                previous_tier_id_rs = statement3.executeQuery(query.toString());

                previous_tier_id_value_part = new StringBuilder(String.valueOf(previous_tier_id_rs.getInt(1)));
                tier++;
            }
            query = new StringBuilder();

            String medal_type_prefix = "";
            String medal_type_relation_table_name = "";
            switch (medal_type) {
                case "Awakening Medals":
                    medal_type_prefix = "awakening";
                    medal_type_relation_table_name = "card_awakening_medal_combination_relation";
                    break;
                case "Dokkan Awakening Medals":
                    medal_type_prefix = "dokkan_awakening";
                    medal_type_relation_table_name = "card_dokkan_awakened_card_relation";
                    break;
                case "Extreme-Z Awakening Medals":
                    medal_type_prefix = "exz_awakening";
                    medal_type_relation_table_name = "card_exz_awakened_card_relation";
                    break;
            }

            query.append("INSERT INTO ").append(medal_type_relation_table_name).append("(card_id,").append(medal_type_prefix)
                    .append("_medal_combination_id) VALUES('").append(card_id).append("',").append(previous_tier_id_value_part).append(")");
            System.out.println(query);
            statement.execute(query.toString());
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void add_transformation_condition(String card_id) {

        List<WebElement> transformation_condition = firefoxDriver.findElements(By.xpath("//a[@title = 'Transformation']//ancestor::tr/following-sibling::tr[1]"));

        if (transformation_condition.size() > 0) {
            try {
                Connection connection;
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
                Statement statement = connection.createStatement();
                String query;
                ResultSet rs;
                int id;
                query = "SELECT count(transformation_condition_id),max(transformation_condition_id) FROM transformation_condition";
                rs = statement.executeQuery(query);

                int transformation_condition_id;
                if (rs.getInt(1) == 0) {
                    transformation_condition_id = 0;
                } else {
                    transformation_condition_id = rs.getInt(2) + 1;
                }
                query = "INSERT INTO transformation_condition(transformation_condition_id,transformation_condition_details) " +
                        "SELECT " + transformation_condition_id + ",'" + formatForSQLite(transformation_condition.get(0).getText()) + "' " +
                        "WHERE NOT EXISTS (SELECT 1 FROM transformation_condition WHERE transformation_condition_details = '" +
                        formatForSQLite(transformation_condition.get(0).getText()) + "')";
                statement.execute(query);

                query = "SELECT transformation_condition_id FROM transformation_condition WHERE transformation_condition_details = '" +
                        formatForSQLite(transformation_condition.get(0).getText()) + "'";
                rs = statement.executeQuery(query);

                query = "INSERT INTO card_transformation_condition_relation(card_id,transformation_condition_id) " +
                        "VALUES ('" + card_id + "'," + rs.getInt(1) + ")";
                statement.execute(query);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private static void add_free_to_play_cards(int version, String card_id) {
        if (!(acquired_from_dokkan_awakening() || acquired_from_exz_awakening(version) || is_invincible_form(version) || is_transformation() || is_or_can_be_exchanged())) {
            String card_name = fetch_card_name(version);
            String xpath = "(//img[contains(@src,'How_to_obtain')])[" + version + "]//ancestor::tr/following-sibling::tr//a[img and not (contains(@href,'png'))]";
            List<WebElement> acquired_links = firefoxDriver.findElements(By.xpath(xpath));
            for (int i = 0; i < acquired_links.size(); i++) {
                String query = "";
                click_element("((//img[contains(@src,'How_to_obtain')])[" + version + "]//ancestor::tr/following-sibling::tr//a[img and not (contains(@href,'png'))])" + "[" + (i + 1) + "]");
                if (is_quest_drop() || is_event_drop(card_name)) {
                    String drop_location = firefoxDriver.findElement(By.xpath("//h1[@class='page-header__title']")).getText();
                    drop_location = drop_location.equals("Quest Mode Guide") ? drop_location.replace("Mode", "").trim() : drop_location;
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Droppable','" + formatForSQLite(drop_location) + "')";
                } else if (is_event_reward(card_name)) {
                    String drop_location = firefoxDriver.findElement(By.xpath("//h1[@class='page-header__title']")).getText();
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Reward','" + formatForSQLite(drop_location) + "')";
                } else if (is_summonable_from_friend_summon()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Summonable','Friend Summon')";
                } else if (is_in_gem_shop()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Purchasable','Incredible Gems Shop')";
                } else if (is_in_babas_points_shop()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Purchasable','Baba''s Points Shop')";
                } else if (is_in_battlefield_memory_shop()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Purchasable','Battlefield Memory Shop')";
                } else if (is_super_battle_road_reward()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Reward','Super Battle Road')";
                } else if (is_login_reward()) {
                    query = "INSERT INTO free_to_play_cards (card_id,availability,origin) VALUES ('" + card_id + "','Reward','Login')";
                }

                if (!query.equals("")) {
                    try {
                        Connection connection;
                        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
                        Statement statement = connection.createStatement();
                        statement.execute(query);
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                browser_back();
                acquired_links = firefoxDriver.findElements(By.xpath("//img[contains(@src,'How_to_obtain')]//ancestor::tr/following-sibling::tr//a[img and not (contains(@href,'png'))]"));
            }
        }
    }

    private static boolean is_quest_drop() {
        List<WebElement> drops = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Quest Mode Guide']"));
        return drops.size() > 0;
    }

    private static boolean is_event_drop(String card_name) {
        List<WebElement> drops = firefoxDriver.findElements(By.xpath("//table[contains(.,'Difficulty') and contains(.,'Boss') and contains(.,'Drop')]//*[a[contains(@title,\"" + card_name + "\")] and position() > 2]"));
        return drops.size() > 0;
    }

    private static boolean is_event_reward(String card_name) {
        List<WebElement> drops = firefoxDriver.findElements(By.xpath("//table[contains(.,'Difficulty') and contains(.,'Boss') and contains(.,'Drop')]//*[a[contains(@title,\"" + card_name + "\")] and position() > 2]"));
        List<WebElement> events = firefoxDriver.findElements(By.xpath("//p/a[contains(@title,'Events')]"));
        return drops.size() == 0 && events.size() > 0;
    }

    private static boolean is_summonable_from_friend_summon() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Friend Summon']"));
        return header.size() > 0;
    }

    private static boolean is_in_gem_shop() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Incredible Gems Shop']"));
        return header.size() > 0;
    }

    private static boolean is_in_babas_points_shop() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()=\"Baba's Points Shop\"]"));
        return header.size() > 0;
    }

    private static boolean is_in_battlefield_memory_shop() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Battlefield Memory Shop']"));
        return header.size() > 0;
    }

    private static boolean is_super_battle_road_reward() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Super Battle Road']"));
        return header.size() > 0;
    }

    private static boolean is_login_reward() {
        List<WebElement> header = firefoxDriver.findElements(By.xpath("//h1[@class='page-header__title' and text()='Login Rewards']"));
        return header.size() > 0;
    }

    private static String formatForSQLite(String text) {
        return text.replaceAll("'", "''");
    }

    private static boolean hasPremierSuperAttack() {
        List<WebElement> elms = firefoxDriver.findElements(By.xpath("//img[contains(@src, 'Special_Skill')]//ancestor::tr/following-sibling::tr//img[contains(@src, 'Premier_Super_Attack_Skill_Effect')]"));
        return elms.size() > 0;
    }

    private static List<WebElement> fetch_ki_meters(int version) {
        List<WebElement> ki_meters = firefoxDriver.findElements(By.xpath("(//img[contains(@src, 'Ki_meter')])[" + version + "]//ancestor::td/following-sibling::td//img"));
        return ki_meters;
    }

    public static void fetch_medals() {

        launch_firefox("https://dbz-dokkanbattle.fandom.com/wiki/Items:_Awakening_Medals");
        accept_cookies();

        List<WebElement> medal_category_elms = firefoxDriver.findElements(By.xpath("//h2//a[contains(@href, '/wiki/')]"));

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
            String medal_title;
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
                temp_medals_count = firefoxDriver.findElements(By.xpath("(//table)[" + i + "]//tr[2]//a")).size();
                for (int j = 1; j <= temp_medals_count; j++) {
                    String xpath = "((//table)[" + i + "]//tr[2]//a)[" + j + "]";
                    temp_medal_elm = firefoxDriver.findElement(By.xpath(xpath));
                    medal_title = temp_medal_elm.getAttribute("title");
                    click_element(xpath);

                    if (!medal_title.contains("Extreme Z-Awakening")) {
                        temp_medal_name = medal_title.replaceAll("Awakening Medals:", "").trim();
                        query = "INSERT INTO awakening_medal (medal_id, medal_name, medal_category) VALUES ("
                                + (total_medals + j - 1) + ",'" + temp_medal_name.replaceAll("'", "''") + "','"
                                + medal_category.get(i - 1).replaceAll("'", "''") + "')";
                        statement.execute(query);

                        temp_image_src = firefoxDriver.findElement(By.xpath("//table[1]//a[img]")).getAttribute("href");
                        while (!downloadImage(temp_image_src, save_path, Integer.toString(total_medals + j - 1))) {
                            browser_refresh();
                        }
                    } else {
                        for (int z = 0; z < 4; z++) {
                            temp_medal_name = medal_title.replaceAll("Extreme Z-Awakening Medals:", "").trim();
                            query = "INSERT INTO awakening_medal (medal_id, medal_name, medal_category) VALUES ("
                                    + (total_medals + j - 1) + ",'" + temp_medal_name.replaceAll("'", "''") +
                                    " " + exz_medal_ranks.get(z) + "','"
                                    + medal_category.get(i - 1).replaceAll("'", "''") + "')";
                            statement.execute(query);

                            temp_image_src = firefoxDriver.findElement(By.xpath("//a[img[contains(@alt,'" + exz_medal_ranks.get(z) + "') or contains(@alt,'" + exz_medal_ranks.get(z).toLowerCase() + "')]]")).getAttribute("href");
                            while (!downloadImage(temp_image_src, save_path, Integer.toString(total_medals + j - 1))) {
                                browser_refresh();
                            }
                            if (z < 3)
                                j++;
                        }
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

        launch_firefox("https://dbz-dokkanbattle.fandom.com/wiki/All_Link_Skills");
        accept_cookies();

        List<WebElement> link_names_elms = firefoxDriver.findElements(By.xpath("//td/a"));
        List<WebElement> link_effects_elms = firefoxDriver.findElements(By.xpath("//tr/td[3]"));

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

            int i = 1;
            Iterator iterator = sorted_links.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry pair = (Map.Entry) iterator.next();

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

    private static void launch_firefox(String link) {

        String firefox_driver_property = "webdriver.gecko.driver";

        if (!System.getProperties().containsKey(firefox_driver_property)) {
            Path firefox_driver_path = Paths.get(System.getProperty("user.dir") + "\\webdrivers\\geckodriver.exe");
            System.setProperty(firefox_driver_property, firefox_driver_path.toString());
            System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        }

        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        options.setCapability("marionette", true);

        firefoxDriver = new FirefoxDriver(options);
        firefoxDriver.manage().timeouts().implicitlyWait(800, TimeUnit.MILLISECONDS);
        firefoxDriver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
        firefoxDriver.get(link);

        waitForLoad();
    }

    static boolean downloadImage(String src, String save_path, String id) {

        boolean succeeded = true;
        try {
            URL imageURL = new URL(src);
            BufferedImage saveImage = ImageIO.read(imageURL);
            ImageIO.write(saveImage, "png", new File(PROJECT_FILEPATH + save_path + id + ".png"));
        } catch (IOException e) {
            succeeded = false;
            e.printStackTrace();
        }
        return succeeded;
    }

    private static void scroll_to_bottom() {
        ((JavascriptExecutor) firefoxDriver).executeScript("window.scrollBy(0,800)");
        waitForLoad();
    }

    private static void accept_cookies() {
        firefoxDriver.findElement(By.xpath("//div[text() = 'ACCEPT']")).click();
        waitForLoad();
    }

    private static void update_error_file() {
        Path error_file_path = Paths.get(PROJECT_FILEPATH + "\\assets\\error_messages.txt");

        Scanner scanner;
        ArrayList<String> content = new ArrayList<>();
        try {
            scanner = new Scanner(new File(error_file_path.toString()));
            scanner.useDelimiter("\n");
            String line;
            while (scanner.hasNext()) {
                line = scanner.next();
                if (!line.equals("\n"))
                    content.add(line.replaceAll("\n+", "\n"));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        content.addAll(error_messages);

        try {
            Files.write(error_file_path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void browser_refresh() {
        try {
            firefoxDriver.navigate().refresh();
            waitForLoad();
        } catch (TimeoutException e) {
            browser_refresh();
        }
    }

    private static void browser_back() {
        try {
            firefoxDriver.navigate().back();
            waitForLoad();
        } catch (TimeoutException e) {
            browser_refresh();
        }
    }

    private static void click_element(String xpath) {

        WebDriverWait wait = new WebDriverWait(firefoxDriver, 20);

        try {
            WebElement elm = firefoxDriver.findElement(By.xpath(xpath));
            wait.until(ExpectedConditions.elementToBeClickable(elm));
            elm.sendKeys(Keys.RETURN);
            waitForLoad();
        } catch (TimeoutException e) {
            System.out.println("refreshing");
            browser_refresh();
            click_element(xpath);
        }
    }

    private static void quit_firefoxDriver() {

        firefoxDriver.close();

        /*try {
            Runtime.getRuntime().exec("taskkill /F /IM geckodriver.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private static void threadSleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void waitForLoad() {

        threadSleep(1000);

        JavascriptExecutor jse = (JavascriptExecutor) firefoxDriver;

        Object result = jse.executeScript("return document.images.length");
        int imagesCount = Integer.parseInt(result.toString());
        for (int i = 0; i < imagesCount; i++) {
            while ((boolean) jse.executeScript("return document.images[" + i + "] == \"undefined\";")) {
                while (!(boolean) jse.executeScript("return document.images[" + i + "].complete")) {
                    threadSleep(500);
                }
                threadSleep(500);
            }
        }

        List<WebElement> home_link = firefoxDriver.findElements(By.xpath("//img[@alt = 'Dragon Ball Z Dokkan Battle Wikia']"));
        if (home_link.size() < 1) {
            System.out.println("refreshing from waitForLoad");
            browser_refresh();
        }
    }
}
