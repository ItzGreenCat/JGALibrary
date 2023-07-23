package me.greencat.galibrary;

import me.greencat.commalization.Commalization;
import me.greencat.commalization.action.ActionType;
import me.greencat.commalization.command.Command;
import me.greencat.commalization.utils.FormattedCommand;
import me.greencat.galibrary.utils.BackgroundPanel;
import me.greencat.galibrary.utils.ScriptReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

public class Screen {
    public final File main;
    public final File menu;
    public final String name;

    public Toolkit toolkit;
    public Dimension resolution;
    public JFrame window;

    public JButton start;
    public JButton load;
    public JButton exit;
    public BackgroundPanel background;

    public JPanel overlay;

    public boolean shouldRunScript = false;

    public File currentFile;
    public File prevFile;

    public JTextArea textDisplayer;

    public CopyOnWriteArrayList<BackgroundPanel> textureRectList = new CopyOnWriteArrayList<>();

    public Commalization menuCommalization = Commalization.getCommalization("menu");

    public Commalization mainCommalization = Commalization.getCommalization("main");

    public String currentText = "";
    public String prevText = "";
    public String currentCharacter = "";

    public Thread readThread = new Thread(() -> {
        while(true){
            try {
                try(FileInputStream inputStream = new FileInputStream(currentFile)){
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if(!shouldRunScript){
                            LockSupport.park();
                        }
                        if(currentFile != prevFile){
                            prevFile = currentFile;
                            break;
                        }
                        GALibrary.getGALibrary().LOGGER.info("当前命令:" + line);
                        FormattedCommand formattedCommand = mainCommalization.format(line);
                        switch(formattedCommand.getCommand()){
                            case "SET_BACKGROUND":
                                window.remove(background);
                                setBackground(formattedCommand.getString("file"));
                                break;
                            case "SET_TEXT":
                                currentText = formattedCommand.getString("text");
                                textDisplayer.setText(currentText);
                                shouldRunScript = false;
                                break;

                        }
                        prevFile = currentFile;
                    }
                    reader.close();
                }

            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    });
    public Screen(String name,String entrance,String menu){
        this.name = name;
        this.main = new File(GALibrary.getGALibrary().gameDir,entrance);
        this.menu = new File(GALibrary.getGALibrary().gameDir,menu);
        currentFile = main;
        prevFile = main;
        if (!this.main.exists()) {
            GALibrary.getGALibrary().LOGGER.severe("无法找到入口点文件");
            throw new RuntimeException();
        }
        if (!this.menu.exists()) {
            GALibrary.getGALibrary().LOGGER.severe("无法找到菜单配置文件");
            throw new RuntimeException();
        }
        loadMainCommalization();
        loadMenuCommalization();
        this.toolkit = Toolkit.getDefaultToolkit();
        resolution = toolkit.getScreenSize();
        setupWindow();
        loadMenu();
        window.setVisible(true);
        readThread.start();
    }
    public void setupWindow(){
        if(window != null){
            window.setVisible(false);
        }
        window = null;
        background = null;
        load = null;
        exit = null;
        start = null;
        textDisplayer = null;
        window = new JFrame(name);
        window.setBounds((int)(resolution.width -(resolution.width * 0.8)) / 2,(int)(resolution.height - (resolution.height * 0.8)) / 2,(int) (resolution.width * 0.8), (int) (resolution.height * 0.8));
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setLayout(null);
    }
    public void loadMenuCommalization(){
        Command addButton = new Command("ADD_BUTTON");
        addButton.addNode("type", ActionType.STRING);
        addButton.addNode("x",ActionType.NUMBER);
        addButton.addNode("y",ActionType.NUMBER);
        addButton.addNode("w",ActionType.NUMBER);
        addButton.addNode("h",ActionType.NUMBER);
        menuCommalization.registerCommand(addButton);
        Command setBackground = new Command("SET_BACKGROUND");
        setBackground.addNode("file", ActionType.STRING);
        menuCommalization.registerCommand(setBackground);
        Command drawRect = new Command("RENDER_RECT");
        drawRect.addNode("file",ActionType.STRING);
        drawRect.addNode("x",ActionType.NUMBER);
        drawRect.addNode("y",ActionType.NUMBER);
        drawRect.addNode("w",ActionType.NUMBER);
        drawRect.addNode("h",ActionType.NUMBER);
        menuCommalization.registerCommand(drawRect);
    }
    public void loadMainCommalization(){
        Command setText = new Command("SET_TEXT");
        setText.addNode("text",ActionType.STRING);
        mainCommalization.registerCommand(setText);
        Command setBackground = new Command("SET_BACKGROUND");
        setBackground.addNode("file",ActionType.STRING);
        mainCommalization.registerCommand(setBackground);
        Command renderCharacter = new Command("RENDER_CHARACTER");
        renderCharacter.addNode("file",ActionType.STRING);
        mainCommalization.registerCommand(renderCharacter);
    }
    public void loadMenu(){
        ScriptReader.read(menu, it -> {
            FormattedCommand formattedCommand = menuCommalization.format(it);
            switch(formattedCommand.getCommand()){
                case "ADD_BUTTON":
                    switch(formattedCommand.getString("type")){
                        case "START":
                            start = new JButton("Start");
                            start.setBounds((int) (formattedCommand.getNumber("x") * window.getWidth()), (int) (formattedCommand.getNumber("y") * window.getHeight()), (int) (formattedCommand.getNumber("w") * window.getWidth()), (int) (formattedCommand.getNumber("h") * window.getHeight()));
                            window.add(start);
                            GALibrary.getGALibrary().LOGGER.info("添加按钮:" + start);
                            break;
                        case "LOAD":
                            load = new JButton("Load");
                            load.setBounds((int) (formattedCommand.getNumber("x") * window.getWidth()), (int) (formattedCommand.getNumber("y") * window.getHeight()), (int) (formattedCommand.getNumber("w") * window.getWidth()), (int) (formattedCommand.getNumber("h") * window.getHeight()));
                            window.add(load);
                            GALibrary.getGALibrary().LOGGER.info("添加按钮:" + load);
                            break;
                        case "EXIT":
                            exit = new JButton("EXIT");
                            exit.setBounds((int) (formattedCommand.getNumber("x") * window.getWidth()), (int) (formattedCommand.getNumber("y") * window.getHeight()), (int) (formattedCommand.getNumber("w") * window.getWidth()), (int) (formattedCommand.getNumber("h") * window.getHeight()));
                            window.add(exit);
                            GALibrary.getGALibrary().LOGGER.info("添加按钮:" + exit);
                            break;
                    }
                    break;
                case "SET_BACKGROUND":
                    File imageFile = new File(GALibrary.getGALibrary().gameDir, formattedCommand.getString("file"));
                    ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                    background = new BackgroundPanel(icon.getImage());
                    background.setBounds(0,0,window.getWidth(),window.getHeight());
                    window.add(background);
                    break;
                case "RENDER_RECT":
                    File textureFile = new File(GALibrary.getGALibrary().gameDir, formattedCommand.getString("file"));
                    ImageIcon textureIcon = new ImageIcon(textureFile.getAbsolutePath());
                    BackgroundPanel rect = new BackgroundPanel(textureIcon.getImage());
                    rect.setBounds((int) (formattedCommand.getNumber("x") * window.getWidth()), (int) (formattedCommand.getNumber("y") * window.getHeight()), (int) (formattedCommand.getNumber("w") * window.getWidth()), (int) (formattedCommand.getNumber("h") * window.getHeight()));
                    textureRectList.add(rect);
                    window.add(rect);
                    break;
            }
        });
        if(exit != null){
            exit.addActionListener((e) -> {
                System.exit(0);
            });
        }
        if(start != null){
            start.addActionListener((e) -> {
                GALibrary.getGALibrary().LOGGER.info("正在加载游戏场景");
                setupWindow();
                loadGameUI();
                window.setVisible(true);
            });
        }
    }
    public void loadGameUI(){
        shouldRunScript = true;
        textDisplayer = new JTextArea();
        setBackground("");
        overlay = new JPanel(){
            @Override
            public void paintComponent(Graphics g){
                super.paintComponents(g);
                g.setColor(new Color(0,0,0,120));
                g.fillRect(0,0,this.getWidth(),this.getHeight());
            }
        };
        overlay.setBounds(0,window.getHeight() / 5 * 3,window.getWidth(),window.getHeight());
        textDisplayer.setBounds(window.getWidth() / 8,window.getHeight() / 5 * 3,window.getWidth() - (window.getWidth() / 8) * 2,window.getHeight());
        textDisplayer.setFocusable(false);
        textDisplayer.setLineWrap(true);
        textDisplayer.setOpaque(false);
        textDisplayer.setForeground(Color.WHITE);
        try {
            InputStream stream = this.getClass().getResourceAsStream("/msyh.ttf");
           textDisplayer.setFont(Font.createFont(Font.TRUETYPE_FONT, convertInputStreamToFile(stream)).deriveFont(64.0F));
        } catch(Exception ignored){

        }
        window.add(textDisplayer);
        window.add(overlay);
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(e.getKeyCode() == KeyEvent.VK_SPACE){
                    shouldRunScript = true;
                    LockSupport.unpark(readThread);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
            }
        });
        LockSupport.unpark(readThread);
    }
    public void setBackground(String backgroundFile){
        File imageFile = new File(GALibrary.getGALibrary().gameDir,backgroundFile);
        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
        background = new BackgroundPanel(icon.getImage());
        background.setBounds(0,0,window.getWidth(),window.getHeight());
        window.add(background);
    }
    class TextDisplayer extends JPanel{
        public void paintComponent(Graphics g) {
            String str = currentText;
            g.setColor(Color.WHITE);
            int offset = 0;
            InputStream stream = this.getClass().getResourceAsStream("/msyh.ttf");
            try {
                this.setFont(Font.createFont(Font.TRUETYPE_FONT,convertInputStreamToFile(stream)).deriveFont(64.0F));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            FontMetrics metrics = getFontMetrics(getFont());
            GALibrary.getGALibrary().LOGGER.info(metrics.toString());
            int counter = 1;
            if(metrics.getStringBounds(str,g).getWidth() > this.getWidth() - (2 * offset)){
                StringBuilder sb = new StringBuilder();
                for(char c : str.toCharArray()){
                    sb.append(c);
                    if(metrics.getStringBounds(sb.toString(),g).getWidth() > this.getWidth() - (2 * offset)){
                        g.drawString(sb.toString(),offset,counter * metrics.getHeight());
                        sb = new StringBuilder();
                        counter++;
                    }
                }
                if(sb.length() != 0){
                    g.drawString(sb.toString(),offset,counter * metrics.getHeight());
                }
            } else {
                g.drawString(str, offset,counter * metrics.getHeight());
            }
        }
    }
    public File convertInputStreamToFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("temp", ".tmp");
        tempFile.deleteOnExit();

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }
}
