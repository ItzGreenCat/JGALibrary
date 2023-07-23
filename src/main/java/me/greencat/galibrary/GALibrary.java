package me.greencat.galibrary;


import me.greencat.commalization.Commalization;
import me.greencat.commalization.action.ActionType;
import me.greencat.commalization.command.Command;
import me.greencat.commalization.utils.FormattedCommand;
import me.greencat.galibrary.utils.ScriptReader;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

public class GALibrary {
    private static GALibrary instance;
    public Logger LOGGER = Logger.getLogger("GALibrary");
    public File gameDir = null;
    public File entryPoint = null;

    public String main;
    public String menu;

    public Commalization gameBootstrap = Commalization.getCommalization("Bootstrap");
    public String gameName;

    public Screen screen;

    public static void main(String[] args) {
        instance = new GALibrary();
        getGALibrary().LOGGER.info(String.format("当前参数:%s",Arrays.toString(args)));
        String gamePath = null;
        try{
            gamePath = args[0];
        } catch(Exception e){
            getGALibrary().LOGGER.severe("无法正确读取参数");
            throw new RuntimeException(e);
        }
        getGALibrary().bootstrap(gamePath);
    }

    public static GALibrary getGALibrary() {
        return instance;
    }

    private void bootstrap(String gamePath){
        GALibrary.getGALibrary().LOGGER.info(String.format("游戏目录:%s",gamePath));
        gameDir = new File(System.getProperty("user.dir"),gamePath);
        GALibrary.getGALibrary().LOGGER.info(String.format("游戏路径:%s",gameDir.getAbsolutePath()));
        if(!gameDir.exists()){
            GALibrary.getGALibrary().LOGGER.severe("无法找到游戏文件夹");
            return;
        }
        File entryPoint = new File(gameDir,"init.gdst");
        if(!entryPoint.exists()){
            GALibrary.getGALibrary().LOGGER.severe("无法找到init.gdst初始化文件");
            return;
        } else {
            this.entryPoint = entryPoint;
        }
        Command setName = new Command("NAME");
        setName.addNode("name", ActionType.STRING);
        gameBootstrap.registerCommand(setName);
        Command main = new Command("MAIN");
        main.addNode("file",ActionType.STRING);
        gameBootstrap.registerCommand(main);
        Command menu = new Command("MENU");
        menu.addNode("file",ActionType.STRING);
        gameBootstrap.registerCommand(menu);

        ScriptReader.read(entryPoint,it -> {
            GALibrary.getGALibrary().LOGGER.info("命令:" + it);
            FormattedCommand formattedCommand = gameBootstrap.format(it);
            switch(formattedCommand.getCommand()){
                case "NAME":
                    gameName = formattedCommand.getString("name");
                    GALibrary.getGALibrary().LOGGER.info("成功加载游戏名:" + gameName);
                    break;
                case "MAIN":
                    this.main = formattedCommand.getString("file");
                    break;
                case "MENU":
                    this.menu = formattedCommand.getString("file");
                    break;
            }
        });
        if (gameName != null && this.main != null && this.menu != null) {
            this.screen = new Screen(gameName,this.main,this.menu);
        } else {
            GALibrary.getGALibrary().LOGGER.info("init.gdst信息不全");
            throw new RuntimeException();
        }
    }
}
