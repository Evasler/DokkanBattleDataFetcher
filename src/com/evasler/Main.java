package com.evasler;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        launch_chrome("https://dbz-dokkanbattle.fandom.com/wiki/All_Link_Skills");
        kill_chrome();
    }

    private static void launch_chrome(String link) {

        String chrome_driver_property = "webdriver.chrome.driver";

        if (!System.getProperties().containsKey(chrome_driver_property)) {
            Path chrome_driver_path = Paths.get(System.getProperty("user.dir") + "\\webdrivers\\chromedriver.exe");
            System.setProperty("webdriver.chrome.driver", chrome_driver_path.toString());
        }

        WebDriver chromeDriver = new ChromeDriver();
        chromeDriver.get(link);

        waitForLoad(chromeDriver);
    }

    private static void kill_chrome() {
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
