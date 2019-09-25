package com.evasler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.imageio.ImageIO;
import javax.sound.midi.Soundbank;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    private static WebDriver chromeDriver;
    private static final  String PROJECT_FILEPATH = System.getProperty("user.dir");
    private static final  String DB_FILEPATH = PROJECT_FILEPATH + "\\assets\\db\\dokkan.db";

    public static void main(String[] args) {

        //fetch_links();
        //fetch_medals();
        //fetch_cards();
        fetch_card();
        quit_chromedriver();
    }

    public static void fetch_cards () {

        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/Items:_Awakening_Medals");
        accept_cookies();

        int cards_on_page = chromeDriver.findElements(By.xpath("//div[@class='category-page__members']//a")).size();
        WebElement temp_card;
        List<WebElement> next_button;

        while (true) {
            next_button = chromeDriver.findElements(By.xpath("//a[contains(@class,'category-page__pagination-next')]"));
            for (int i = 1; i <= cards_on_page; i++) {
                temp_card = chromeDriver.findElement(By.xpath("(//div[@class='category-page__members']//a)[" + i + "]"));
                click_element(temp_card);
                fetch_card();
                browser_back();
            }
            if (next_button.size() > 0) {
                click_element(next_button.get(0));
            } else {
                break;
            }
        }

    }

    public static void fetch_card() {

        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/Special_Transformation_Super_Saiyan_3_Gotenks");
        accept_cookies();
        scroll_to_bottom();



        String card_id = fetch_card_id(1);
        /*System.out.println("card_id: " + fetch_card_id(1));
        System.out.println("card_name: " + fetch_card_name());
        System.out.println("character_name: " + fetch_character_name());
        System.out.println("rarity: " + fetch_rarity());
        System.out.println("type: " + fetch_type());
        System.out.println("cost: " + fetch_cost());
        System.out.println("max_sa_lvl: " + fetch_max_sa_lvl());
        System.out.println("leader_skill: " + fetch_skill_info("Leader Skill",1));
        System.out.println("passive_skill_name: " + fetch_skill_info("Passive Skill Name",1));
        System.out.println("passive_skill: " + fetch_skill_info("Passive Skill",1));
        System.out.println("active_skill_name: " + fetch_skill_info("Active Skill Name",1));
        System.out.println("active_skill: " + fetch_skill_info("Active Skill",1));
        List<String> stats = fetch_max_stats(1);
        System.out.println("HP: " + stats.get(0));
        System.out.println("DEF: " + stats.get(1));
        System.out.println("ATK: " + stats.get(2));
        System.out.println("multiplier: " + fetch_twelve_ki_multiplier());
        System.out.println("hidden_potential_rank: " + fetch_hidden_potential_rank());*/
        add_super_attack(1, card_id);
    }

    private static String fetch_card_id(int version) {
        String new_card_id = null;
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query;
            ResultSet rs;

            if (!is_japanese_version()) {
                if (acquired_from_exz_awakening(version) || is_giant_form(version) || is_transformation() || has_been_exchanged()) {
                    String card_id = fetch_existing_card_id_by_card_name();
                    new_card_id = Integer.toString(fetch_largest_line_card_id_used(card_id) + 1);
                } else if (acquired_from_dokkan_awakening(version)) {
                    WebElement link = chromeDriver.findElement(By.xpath("(//table[contains(.,'How_to_obtain.png')])[" + version + "]//img[contains(@src, 'Dokkan_awaken')]/parent::a/following-sibling::a[1]"));

                    click_element(link);
                    String pre_dokkan_awakening_card_id = fetch_existing_card_id_by_card_name();
                    browser_back();

                    new_card_id = Integer.toString(fetch_largest_line_card_id_used(pre_dokkan_awakening_card_id) + 1);
                } else {
                    query = "SELECT max(card_id) FROM card WHERE card_id NOT LIKE '%_jp'";
                    rs = statement.executeQuery(query);
                    String max_card_id = null;
                    while(rs.next()) {
                        max_card_id = rs.getString(1);
                    }

                    if (max_card_id == null) {
                        max_card_id = "0";
                        new_card_id = max_card_id;
                    } else {
                        int max_card_id_value = Integer.parseInt(max_card_id);
                        new_card_id = Integer.toString(max_card_id_value + 20 - (max_card_id_value % 20));
                    }
                }
            }

            if (is_japanese_version()) {
                String card_id = fetch_existing_card_id_by_card_name();
                if (acquired_from_exz_awakening(version)) {
                    query = "SELECT exz_awakened_card_id FROM card_exz_awakened_card_relation WHERE card_id = '" + card_id + "'";
                    rs = statement.executeQuery(query);
                    while(rs.next()) {
                        new_card_id = rs.getString(1) + "_jp";
                    }
                    if (new_card_id == null) {
                        new_card_id = Integer.toString(fetch_largest_line_card_id_used(card_id) + 1) + "_jp";
                    }
                } else {
                    assert false;
                }
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        assert new_card_id != null;
        return new_card_id;
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
            String query = "CREATE TABLE `TempTable` (`card_id`\tTEXT);\n" +
                    "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(card_id) FROM card WHERE card_id >= " + line_base_card_id + " AND card_id <= " + line_max_card_id + ";\n" +
                    "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(exz_awakened_card_id) FROM exz_awakened_card WHERE exz_awakened_card_id >= " + line_base_card_id + " AND exz_awakened_card_id <= " + line_max_card_id + ";\n" +
                    "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(giant_form_card_id) FROM giant_form_card WHERE giant_form_card_id >= " + line_base_card_id + " AND giant_form_card_id <= " + line_max_card_id + ";\n" +
                    "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(transformation_card_id) FROM transformation_card WHERE transformation_card_id >= " + line_base_card_id + " AND transformation_card_id <= " + line_max_card_id + ";\n" +
                    "INSERT INTO TempTable(card_id)\n" +
                    "SELECT max(exchange_card_id) FROM exchange_card WHERE exchange_card_id >= " + line_base_card_id + " AND exchange_card_id <= " + line_max_card_id + ";\n" +
                    "SELECT max(card_id) FROM TempTable;\n" +
                    "DROP TABLE TempTable;";
            rs = statement.executeQuery(query);

            while(rs.next()) {
                largest_line_card_id_used_value = Integer.parseInt(rs.getString(1).replace("_jp", ""));
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return largest_line_card_id_used_value;
    }

    private static String fetch_existing_card_id_by_card_name() {

        String card_id = null;
        String card_name = fetch_card_name();

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILEPATH);
            Statement statement = connection.createStatement();
            String query = "SELECT card_id FROM card WHERE card_name = '" + card_name + "' AND card_id NOT LIKE '%_jp'";

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

    private static boolean acquired_from_dokkan_awakening(int version){
        List<WebElement> dokkan_awaken_logo = chromeDriver.findElements(By.xpath("(//img[contains(@src, 'How_to_obtain')])[" + version + "]//ancestor::tr[1]/following-sibling::tr//img[contains(@src, 'Dokkan_awaken')]"));
        return dokkan_awaken_logo.size() > 0;
    }

    private static boolean acquired_from_exz_awakening(int version){
        List<WebElement> exz_awakening_button = chromeDriver.findElements(By.xpath("(//div[@title = 'Transcended Ultra Rare'])[" + version + "]/preceding-sibling::ul//a[@title = 'Extreme Z-Awakened']/parent::li"));
        return exz_awakening_button.size() > 0 && exz_awakening_button.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean is_giant_form(int version) {
        List<WebElement> giant_form_category = chromeDriver.findElements(By.xpath("//img[contains(@src, 'Category')]//ancestor::tr[1]/following-sibling::tr//a[@title = 'Giant Form']"));
        List<String> stats = fetch_max_stats(version);
        return giant_form_category.size() > 0 && stats.get(0).equals("∞") && stats.get(2).equals("∞");
    }

    private static boolean is_transformation() {
        List<WebElement> transformation_skill = chromeDriver.findElements(By.xpath("(//img[contains(@src, 'Special_Skill')])[1]//ancestor::tr/following-sibling::tr//a[@title = 'Transformation']"));
        List<WebElement> form_tabs = chromeDriver.findElements(By.xpath("(//ul[@class = 'tabbernav'])[1]//li"));

        return transformation_skill.size() > 0 && form_tabs.size() > 0 && !form_tabs.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean has_been_exchanged() {
        List<WebElement> form_tabs = chromeDriver.findElements(By.xpath("(//ul[@class = 'tabbernav'])[1]//li"));
        String active_skill_name = fetch_skill_info("Active Skill Name", 1);
        return active_skill_name != null && active_skill_name.equals("Exchange") && form_tabs.size() > 0 && !form_tabs.get(0).getAttribute("class").equals("tabberactive");
    }

    private static boolean is_japanese_version() {
        List<WebElement> version = chromeDriver.findElements(By.xpath("//a[@title = 'Japan']/parent::li"));
        return version.size() > 0 && version.get(0).getAttribute("class").equals("tabberactive");
    }

    private static String fetch_card_name() {
        String text = chromeDriver.findElement(By.xpath("//table//tr//center//b[br]")).getText();
        return text.substring(0, text.indexOf("\n")).trim();
    }

    private static String fetch_character_name() {
        String text = chromeDriver.findElement(By.xpath("//table//tr//center//b[br]")).getText();
        return text.substring(text.indexOf("\n") + 1).trim();
    }

    private static String fetch_rarity() {
        return chromeDriver.findElement(By.xpath("(//table)[1]//tr[3]/td[3]//a[1]")).getAttribute("title").replace("Category:", "");
    }

    private static String fetch_type() {
        return chromeDriver.findElement(By.xpath("(//table)[1]//tr[3]/td[4]//a[last()]")).getAttribute("title").replace("Category:", "");
    }

    private static int fetch_cost() {
        String text = chromeDriver.findElement(By.xpath("(//table)[1]//tr[3]/td[5]")).getText();
        if (text.contains("/"))
            text = text.substring(text.indexOf("/") + 1);
        return Integer.parseInt(text);
    }

    private static int fetch_max_sa_lvl() {
        String text = chromeDriver.findElement(By.xpath("(//table)[1]//tr[3]/td[2]")).getText();
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
        List<WebElement> element = chromeDriver.findElements(By.xpath(xpath));

        return element.size() > 0 ? clean_text(element.get(0).getAttribute("innerHTML"), png, amendment, version) : null;
    }

    private static String clean_text(String text, String png, String amendment, int version) {
        text = clean_from_icons(text, png, amendment, version);
        text = clean_from_links(text, png, amendment, version);
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
            br_replacement =" ";
        }

        text = text.replaceAll("<br>", br_replacement);
        text = text.replaceAll("<[^<>]+>", "");
        text = text.replaceAll("</[^<>]+>", "");
        return text;
    }

    public static String clean_from_links(String text, String png, String amendment, int version) {
        List<WebElement> links = chromeDriver.findElements(By.xpath("((//img[contains(@src,'" + png + ".png')])[" + version + "]" + amendment + "//a[not (img)]"));
        for (WebElement link: links) {
            text = text.replace(link.getAttribute("outerHTML"), link.getText());
        }
        return text;
    }

    public static String clean_from_icons(String text, String png, String amendment, int version) {
        List<WebElement> icons = chromeDriver.findElements(By.xpath("((//img[contains(@src,'" + png + ".png')])[" + version + "]" + amendment + "//img"));
        for (WebElement icon: icons) {
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
        return fetch_stats(version, 2);
    }

    private static List<String> fetch_max_rainbow_stats(int version) {
        return fetch_stats(version, 4);
    }

    private static List<String> fetch_stats(int version, int column) {
        List<WebElement> elms = chromeDriver.findElements(By.xpath("(//table[contains(.,'Stats.png')])[" + version + "]//td[not (contains(.,'png'))][" + column + "]"));
        List<String> stats = new ArrayList<>();
        for (WebElement elm : elms) {
            stats.add(elm.getText());
        }
        return stats;
    }

    private static Integer fetch_twelve_ki_multiplier() {
        String text = chromeDriver.findElement(By.xpath("((//table[contains(.,'Additional Information.png')])[1]//p[contains(.,'Multiplier')])[1]")).getText();

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

    private static String fetch_hidden_potential_rank() {
        List<String> max_stats = fetch_max_stats(1);
        List<String> max_rainbow_stats = fetch_max_rainbow_stats(1);

        int hp_boost = Integer.parseInt(max_rainbow_stats.get(0)) - Integer.parseInt(max_stats.get(0));
        int atk_boost = Integer.parseInt(max_rainbow_stats.get(1)) - Integer.parseInt(max_stats.get(1));
        int def_boost = Integer.parseInt(max_rainbow_stats.get(2)) - Integer.parseInt(max_stats.get(2));

        String type = fetch_type();
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
            while(rs.next()) {
                hidden_potential_rank = rs.getString(1);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hidden_potential_rank;
    }

    private static String fetch_drop_event() {
        return chromeDriver.findElement(By.xpath("//img[contains(@src, 'How_to_obtain')]//ancestor::tr/following-sibling::tr//a")).getAttribute("title");
    }

    private static List<String> add_super_attack(int version, String card_id) {

        List<String> super_attack_name_contents = new ArrayList<>(Arrays.asList(Objects.requireNonNull(fetch_skill_info("Super Attack Name", version)).split("\n")));
        List<String> super_attack_names = new ArrayList<>();
        List<String> super_attack_types = new ArrayList<>();
        List<String> super_attack_launch_condition = new ArrayList<>();
        String ki;
        for (String content : super_attack_name_contents) {
            ki = "";
            content = content.trim();
            if (fetch_ki_meters().size() > 1 ||(content.contains("(") && !content.matches("[^()]+(\\(Extreme\\)\\s)?\\([0-9]{1,2}-?[0-9]{1,2}\\+?\\sKi\\)$") &&
                    !content.matches("[^()]+?(\\(Extreme\\))?$"))) {
                System.out.println("Irregular SA: " + fetch_card_name() + "\n---------------------------------------------------------");
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
                        System.out.println("Premier SA: " + fetch_card_name() + "\n---------------------------------------------------------");
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

        return null;
    }

    private static String formatForSQLite(String text) {
        return text.replaceAll("'", "''");
    }

    private static boolean hasPremierSuperAttack() {
        List<WebElement> elms = chromeDriver.findElements(By.xpath("//img[contains(@src, 'Special_Skill')]//ancestor::tr/following-sibling::tr//img[contains(@src, 'Premier_Super_Attack_Skill_Effect')]"));
        return elms.size() > 0;
    }

    private static List<WebElement> fetch_ki_meters() {
        List<WebElement> ki_meters = chromeDriver.findElements(By.xpath("//img[contains(@src, 'Ki_meter')]//ancestor::td/following-sibling::td//img"));
        return ki_meters;
    }

    public static void fetch_medals() {

        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/Items:_Awakening_Medals");
        accept_cookies();

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
        accept_cookies();

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

            int i = 1;
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

    private static void scroll_to_bottom() {
        ((JavascriptExecutor) chromeDriver).executeScript("window.scrollBy(0,500)");
        waitForLoad();
    }

    private static void accept_cookies() {
        chromeDriver.findElement(By.xpath("//div[text() = 'ACCEPT']")).click();
        waitForLoad();
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

    private static void quit_chromedriver() {

        chromeDriver.quit();

        /*try {
            Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    static void waitForLoad() {

        chromeDriver.manage().timeouts().implicitlyWait(2000, TimeUnit.MILLISECONDS);
        new WebDriverWait(chromeDriver, 5000).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
    }
}
