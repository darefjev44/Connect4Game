package Game;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * The driver class for my Connect Four game.
 * @author Daniel Arefjev
 */
public class Connect4Game extends JFrame implements MouseListener, ActionListener {
    GameBoard gameBoard;
    JMenu gameMenu;
    GameTimer timer;
    JFrame settingsMenu;
    GridLayout mainLayout;
    ArrayList<SimpleGameBoard> gameHistory = new ArrayList<>();
    File gameHistoryFile = new File("game_history.c4g");
    File selectedFile;
    FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("Connect 4 Save Files", "c4g");

    //imageIcons
    ImageIcon red = new ImageIcon("Connect4Game/Images/RED.gif");
    ImageIcon blue = new ImageIcon("Connect4Game/Images/BLUE.gif");
    ImageIcon cyan = new ImageIcon("Connect4Game/Images/CYAN.gif");
    ImageIcon green = new ImageIcon("Connect4Game/Images/GREEN.gif");
    ImageIcon magenta = new ImageIcon("Connect4Game/Images/MAGENTA.gif");
    ImageIcon yellow = new ImageIcon("Connect4Game/Images/YELLOW.gif");
    ImageIcon orange = new ImageIcon("Connect4Game/Images/ORANGE.gif");
    ImageIcon[] imageIcons = {red, blue, cyan, green, magenta, yellow, orange};

    //settings fields and stuff
    JCheckBox aiToggle;
    JComboBox aiDifficulty;
    String aiDifficultyOptions[] = {"Very Easy", "Easy"};
    JTextField p1Name;
    JTextField p2Name;
    String pColorOptions[] = {"Red", "Blue", "Cyan", "Green", "Magenta", "Yellow", "Orange"};
    JComboBox p1Colour;
    JComboBox p2Colour;
    SpinnerNumberModel bSizeModel;
    JSpinner bSize;
    JButton settingsStartButton;

    //other UI elements
    TitledBorder title;
    JPanel panel;
    JPanel gamePanel;
    JLabel currentPlayerName;
    JButton mainStartButton;
    JFileChooser fileChooser;

    //main UI

    /**
     * Method to create the main game window and the menu bar.
     */
    public Connect4Game(){
        super("Connect 4 Game");
        mainLayout = new GridLayout();
        setLayout(mainLayout);

        createGameMenu();
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        menuBar.setLayout(new BorderLayout());
        menuBar.add(gameMenu, BorderLayout.WEST);

        currentPlayerName = new JLabel();
        menuBar.add(currentPlayerName, BorderLayout.EAST);
        currentPlayerName.setVisible(false);
        //not the cleanest solution, but JSeparator was acting very strangely with the menu bar. this looks nice anyways.
        currentPlayerName.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        timer = new GameTimer();
        menuBar.add(timer);

        createStartArea();

        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Method to create the top left menu and add its items.
     */
    public void createGameMenu(){
        JMenuItem item;
        gameMenu = new JMenu("Menu");

        item = new JMenuItem("New Game");
        item.addActionListener(this);
        gameMenu.add(item);

        item = new JMenuItem("Save Game");
        item.addActionListener(this);
        gameMenu.add(item);

        item = new JMenuItem("Load Game");
        item.addActionListener(this);
        gameMenu.add(item);

        JSeparator separator = new JSeparator();
        gameMenu.add(separator);

        item = new JMenuItem("View History");
        item.addActionListener(this);
        gameMenu.add(item);
    }

    /**
     * Method to reset and create the "start" area for the main game window.
     */
    public void createStartArea(){
        //getting rid of any now unnecessary objects
        if(gamePanel != null){
            remove(gamePanel);
        }
        if(gameBoard != null){
            gameBoard = null;
        }
        if(timer != null){
            timer.setVisible(false);
        }

        /* I didn't intend for the button to take up the full area, but I actually like the look of it. */
        gamePanel = new JPanel();
        gamePanel.setLayout(new BorderLayout());

        mainStartButton = new JButton("Start Game");
        mainStartButton.addActionListener(new ButtonEventHandler());
        gamePanel.add(mainStartButton, BorderLayout.CENTER);

        add(gamePanel);
    }

    /**
     * Method to reset the main window area and populate it with the actual game elements.
     */
    public void createGameArea(){
        remove(gamePanel);

        int boardSize = (int)bSize.getValue();

        if(timer!=null){
            getJMenuBar().remove(timer);
            timer = null;
            timer = new GameTimer();
            getJMenuBar().add(timer);
        }

        gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(boardSize, boardSize));

        //ensuring players don't get the same colours
        int p1index = p1Colour.getSelectedIndex();
        int p2index = p2Colour.getSelectedIndex();
        if(p1index == p2index){
            while(p1index == p2index){
                p2index = (int)(Math.random()*(imageIcons.length - 1));
            }
            p2Colour.setSelectedIndex(p2index);
            JOptionPane.showMessageDialog(null, "Players cannot be the same colour. Setting player 2 to " + pColorOptions[p2index] + ".");
        }

        timer.setVisible(true);
        timer.startTimer();

        gameBoard = new GameBoard(boardSize);
        gameBoard.setTimeStarted(new GregorianCalendar());
        gameBoard.setPlayerIcons(imageIcons[p1index], imageIcons[p2index]);
        gameBoard.setAIStuff(aiToggle.isSelected(), aiDifficulty.getSelectedIndex());
        gameBoard.setPlayerNames(p1Name.getText(), p2Name.getText());

        for(int i = 0; i < boardSize; i++){
            for(int j = 0; j < boardSize; j++){
                gamePanel.add(gameBoard.getGameBoard()[j][i]);
                gameBoard.getGameBoard()[j][i].addMouseListener(this);
            }
        }

        settingsMenu.setVisible(false);

        updatePlayerLabel();
        currentPlayerName.setVisible(true);
        /*
        need to poke the main window for it to update for some reason - otherwise borders aren't displayed
        goal with size is for the 7x7 (default) board size to result in window being roughly 500x500
        this might be too big on standard resolutions though as I'm running in 3440*1440
        needs further testing - could possibly grab the user's monitor resolution and do something with that if necessary.
         */
        add(gamePanel);
        this.setSize(72*boardSize, 72 * boardSize + getJMenuBar().getHeight());
    }

    /**
     * Method to create the settings menu which can be accessed from the Game menu within the menu bar.
     * Here you can select various options to start a new game with.
     */
    public void createSettingsMenu(){
        settingsMenu = new JFrame("Game Settings");

        GridBagLayout layout = new GridBagLayout();
        settingsMenu.setLayout(layout);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 10, 0, 10);
        settingsMenu.setSize(300, 550);

        c.weighty = 1;
        c.weightx = 1;
        //ai
        c.gridx = 0;
        c.gridy = 0;
        createAISettings();
        settingsMenu.add(panel, c);

        //players
        c.gridx = 0;
        c.gridy = 1;
        createPlayerSettings();
        settingsMenu.add(panel, c);

        //board settings
        c.gridx = 0;
        c.gridy = 2;
        createBoardSettings();
        settingsMenu.add(panel, c);

        c.gridx = 0;
        c.gridy = 3;
        settingsStartButton = new JButton("Start Game");
        settingsStartButton.addActionListener(new ButtonEventHandler());
        settingsMenu.add(settingsStartButton, c);
    }

    /**
     * Method which creates a panel with board related settings to be included in the settings menu.
     */
    public void createBoardSettings(){
        /* found out about JSpinner and looked it up on the java docs, which also lead me to SpinnerModel/SpinnerNumberModel */
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        title = BorderFactory.createTitledBorder("Board");
        panel.setBorder(title);

        JLabel label;
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 0, 10, 0);

        bSizeModel = new SpinnerNumberModel(7, 4, 24, 1);
        //board size
        //label
        label = new JLabel("Board Size");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(label, c);
        //spinner
        bSize = new JSpinner(bSizeModel);
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(bSize, c);
        bSize.addChangeListener(e -> {
            /* ensures the board size input doesn't exceed mins/maxes and removes the need for validation elsewhere.
             * for some reason getMaximum must be cast to a Number before it can be cast to an int, discovered this solution
             * after seeing a code snippet here: https://www.programcreek.com/java-api-examples/?api=javax.swing.SpinnerNumberModel
             * (example 19)
             */
            int value, max, min;
            Number maxAsNum, minAsNum;
            if(e.getSource() == bSize){
                for(int i = 0; i < bSize.getValue().toString().length(); i++){
                    if(!Character.isDigit(bSize.getValue().toString().charAt(i))){
                        bSize.setValue(7);
                    }
                }
                value = (int)bSize.getValue();
                maxAsNum = (Number)bSizeModel.getMaximum();
                minAsNum = (Number)bSizeModel.getMinimum();
                max = (int)maxAsNum;
                min = (int)minAsNum;
                if(value > max){
                    bSize.setValue(max);
                } else if(value < min){
                    bSize.setValue(min);
                }
            }
        });
    }
    /**
     * Method which creates a panel with AI related settings to be included in the settings menu.
     */
    public void createAISettings(){
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        title = BorderFactory.createTitledBorder("AI");
        panel.setBorder(title);

        JLabel label;
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 0, 10, 0);

        label = new JLabel("Play VS AI?");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(label, c);
        aiToggle = new JCheckBox();
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(aiToggle, c);
        aiToggle.addActionListener(this);

        label = new JLabel("AI Difficulty");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        panel.add(label, c);
        aiDifficulty = new JComboBox(aiDifficultyOptions);
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 1;
        panel.add(aiDifficulty, c);
        aiDifficulty.addActionListener(this);
    }
    /**
     * Method which creates a panel with player related settings to be included in the settings menu.
     */
    public void createPlayerSettings(){
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        title = BorderFactory.createTitledBorder("Players");
        panel.setBorder(title);

        JLabel label;
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 0, 10, 0);

        label = new JLabel("Player 1 Name");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(label, c);
        p1Name = new JTextField();
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(p1Name, c);
        p1Name.addActionListener(this);

        label = new JLabel("Player 2 Name");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        panel.add(label, c);
        p2Name = new JTextField();
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 1;
        panel.add(p2Name, c);
        p2Name.addActionListener(this);

        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(220,1));
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        panel.add(separator, c);

        c.insets = new Insets(10, 0, 10, 0);
        c.gridwidth = 1;
        label = new JLabel("Player 1 Colour");
        c.gridx = 0;
        c.gridy = 3;
        panel.add(label, c);
        p1Colour = new JComboBox(pColorOptions);
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 3;
        panel.add(p1Colour, c);
        p1Colour.addActionListener(this);

        label = new JLabel("Player 2 Colour");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 4;
        panel.add(label, c);
        p2Colour = new JComboBox(pColorOptions);
        c.weightx = 2;
        c.gridx = 1;
        c.gridy = 4;
        panel.add(p2Colour, c);
        p2Colour.addActionListener(this);

        //just making sure all the bigger fields are the same size and it looks neat :-)
        p1Name.setPreferredSize(p1Colour.getPreferredSize());
        p2Name.setPreferredSize(p1Colour.getPreferredSize());
    }

    /**
     * Main method which just creates an instance of the Connect4Game JFrame.
     */
    public static void main(String[] args){
        Connect4Game game = new Connect4Game();
    }

    //load/save game

    /**
     * Method to load a save file which contains a SimpleGameBoard object, and convert it into a GameBoard object using
     * a JFileChooser.
     */
    public void loadGame() throws IOException, ClassNotFoundException {
        selectedFile = null; //clearing the selected file before opening file chooser again
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(fileFilter);
        int status = fileChooser.showOpenDialog(null);
        if(status == JFileChooser.APPROVE_OPTION){
            selectedFile = fileChooser.getSelectedFile();
        }

        try{
            if(selectedFile != null){
                SimpleGameBoard simpleGameBoard;

                FileInputStream inputStream = new FileInputStream(selectedFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                simpleGameBoard = (SimpleGameBoard) objectInputStream.readObject();

                if(timer!=null){
                    getJMenuBar().remove(timer);
                    timer = null;
                    timer = new GameTimer();
                    getJMenuBar().add(timer);
                }

                remove(gamePanel);

                int boardSize = simpleGameBoard.getBoardSize();

                gamePanel = new JPanel();
                gamePanel.setLayout(new GridLayout(boardSize, boardSize));

                timer.setVisible(true);
                timer.setTimeElapsed(simpleGameBoard.getTimeElapsed());
                timer.startTimer();

                gameBoard = new GameBoard(boardSize);
                gameBoard.setPlayerIcons(imageIcons[simpleGameBoard.getPlayer1Icon()], imageIcons[simpleGameBoard.getPlayer2Icon()]);
                gameBoard.setAIStuff(simpleGameBoard.getAIToggle(), simpleGameBoard.getAiDifficulty());
                gameBoard.setPlayerNames(simpleGameBoard.getPlayer1Name(), simpleGameBoard.getPlayer2Name());
                gameBoard.setLastAIMove(simpleGameBoard.getLastAIMove());
                gameBoard.setAIStuff(simpleGameBoard.getAIToggle(), simpleGameBoard.getAiDifficulty());
                gameBoard.setPlayer(simpleGameBoard.getPlayer());
                gameBoard.setTimeStarted(simpleGameBoard.getTimeStarted());
                gameBoard.setPlayerIcons(imageIcons[simpleGameBoard.getPlayer1Icon()], imageIcons[simpleGameBoard.getPlayer2Icon()]);

                for(int i = 0; i < boardSize; i++){
                    for(int j = 0; j < boardSize; j++){
                        gamePanel.add(gameBoard.getGameBoard()[j][i]);
                        gameBoard.getGameBoard()[i][j].setState(simpleGameBoard.getGameState()[i][j]);
                        gameBoard.getGameBoard()[j][i].addMouseListener(this);
                    }
                }

                gameBoard.loadIcons();

                /*
                Unnecessary, not sure why I put this in.
                If I wanted to keep it though, it just needs to be wrapped in an if(settingsMenu!=null) statement.
                 */
                // settingsMenu.setVisible(false);

                updatePlayerLabel();
                currentPlayerName.setVisible(true);

                add(gamePanel);
                this.setSize(72*boardSize, 72 * boardSize + getJMenuBar().getHeight());
            } else {
                JOptionPane.showMessageDialog(null, "No file was selected.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (FileNotFoundException fileNotFoundException){
            JOptionPane.showMessageDialog(null, "File could not be found.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ioException){
            JOptionPane.showMessageDialog(null, "File could not be read.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method to convert the currently used GameBoard object into a SimpleGameBoard object and to save it to a file
     * using a JFileChooser. There is also some code to ensure that it's saved as a .c4g file.
     */
    public void saveGame() throws IOException {
        int boardSize = gameBoard.getGameBoard().length;

        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setApproveButtonText("Save");
        int status = fileChooser.showOpenDialog(null);
        if(status == JFileChooser.APPROVE_OPTION){
            selectedFile = fileChooser.getSelectedFile();

            //ensuring file is .c4g extension
            String selectedFileName = selectedFile.getAbsolutePath();
            String fileExtension = selectedFileName.substring(selectedFileName.length()-4, selectedFileName.length());
            if(!fileExtension.equals(".c4g")){
                selectedFile = new File(selectedFileName + ".c4g");
            }
        }

        try {
            if (selectedFile != null) {
                int[][] boardStateAsInt = new int[boardSize][boardSize];
                for(int i = 0; i<boardSize; i++){
                    for(int j = 0; j<boardSize; j++){
                        boardStateAsInt[i][j] = gameBoard.getGameBoard()[i][j].getState();
                    }
                }
                SimpleGameBoard simpleGameBoard = new SimpleGameBoard(gameBoard.getGameBoard().length, boardStateAsInt);
                simpleGameBoard.setAIStuff(gameBoard.getAIToggle(), gameBoard.getAiDifficulty());
                simpleGameBoard.setPlayerIcons(p1Colour.getSelectedIndex(), p2Colour.getSelectedIndex());
                simpleGameBoard.setPlayerNames(gameBoard.getPlayer1Name(), gameBoard.getPlayer2Name());
                simpleGameBoard.setLastAIMove(gameBoard.getLastAIMove());
                simpleGameBoard.setPlayer(gameBoard.getPlayer());
                simpleGameBoard.setTimeStarted(gameBoard.getTimeStarted());
                simpleGameBoard.setTimeElapsed(timer.getTimeElapsed());

                FileOutputStream outputStream = new FileOutputStream(selectedFile);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(simpleGameBoard);
            } else {
                JOptionPane.showMessageDialog(null, "No file was selected.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (FileNotFoundException fileNotFoundException){
            JOptionPane.showMessageDialog(null, "File could not be found.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ioException){
            JOptionPane.showMessageDialog(null, "File could not be written.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method which is called when a game ends, which converts the current GameBoard to a SimpleGameBoard and stores
     * it in an ArrayList<SimpleGameBoard> within the game_history.c4g file for viewing in the history window.
     */
    public void saveToHistory() throws IOException {
        int boardSize = gameBoard.getGameBoard().length;

        int[][] boardStateAsInt = new int[boardSize][boardSize];
        for(int i = 0; i<boardSize; i++){
            for(int j = 0; j<boardSize; j++){
                boardStateAsInt[i][j] = gameBoard.getGameBoard()[i][j].getState();
            }
        }
        SimpleGameBoard simpleGameBoard = new SimpleGameBoard(gameBoard.getGameBoard().length, boardStateAsInt);
        simpleGameBoard.setAIStuff(gameBoard.getAIToggle(), gameBoard.getAiDifficulty());
        simpleGameBoard.setPlayerIcons(p1Colour.getSelectedIndex(), p2Colour.getSelectedIndex());
        simpleGameBoard.setPlayerNames(gameBoard.getPlayer1Name(), gameBoard.getPlayer2Name());
        simpleGameBoard.setLastAIMove(gameBoard.getLastAIMove());
        simpleGameBoard.setWinner(gameBoard.getWinner());
        simpleGameBoard.setPlayer(gameBoard.getPlayer());
        simpleGameBoard.setTimeStarted(gameBoard.getTimeStarted());
        simpleGameBoard.setTimeElapsed(gameBoard.getTimeElapsed());
        //System.out.println("SimpleGameboard: " + simpleGameBoard.getTimeElapsed().get(Calendar.SECOND));

        gameHistory.add(simpleGameBoard);

        FileOutputStream outputStream = new FileOutputStream(gameHistoryFile);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(gameHistory);
        outputStream.close();
    }

    /**
     * Method which is called when the history menu is opened, which loads the contents of the game_history.c4g
     * file to be displayed in the history menu.
     */
    public void loadHistory() throws IOException, ClassNotFoundException {
        FileInputStream inputStream = new FileInputStream(gameHistoryFile);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        gameHistory = (ArrayList<SimpleGameBoard>) objectInputStream.readObject();
        inputStream.close();

        JFrame historyWindow = new JFrame("Game History");
        ArrayList<JPanel> historyPanels = new ArrayList<>();
        GridLayout layout = new GridLayout(0, 1);
        JPanel historyContainer = new JPanel();
        historyContainer.setLayout(layout);
        JScrollPane jScrollPane = new JScrollPane(historyContainer);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel panel;
        JLabel player1Label;
        JLabel player2Label;
        JLabel timeElapsedLabel;
        JLabel timePlayedLabel;
        SimpleDateFormat dateFormat;
        GridBagLayout entryLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        for(int i = 0; i < gameHistory.size(); i++){
            player1Label = new JLabel(gameHistory.get(i).getPlayer1Name());
            player2Label = new JLabel(gameHistory.get(i).getPlayer2Name());

            if(gameHistory.get(i).getWinner() == 1){
                player1Label.setText("<html><u>" + player1Label.getText() + "</html></u>");
            } else if (gameHistory.get(i).getWinner() == 2){
                player2Label.setText("<html><u>" + player2Label.getText() + "</html></u>");
            }

            timeElapsedLabel = new JLabel("Time elapsed: " + String.format("%02d:%02d", gameHistory.get(i).getTimeElapsed().get(Calendar.MINUTE), gameHistory.get(i).getTimeElapsed().get(Calendar.SECOND)));
            dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm");
            timePlayedLabel = new JLabel("Date: " + dateFormat.format(gameHistory.get(i).getTimeStarted().getTime()));
            panel = new JPanel();
            panel.setLayout(entryLayout);

            c.gridwidth = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.anchor = GridBagConstraints.EAST;
            panel.add(player1Label, c);
            c.gridx = 1;
            c.weightx = 0.5;
            c.anchor = GridBagConstraints.CENTER;
            panel.add(new JLabel(" vs "), c);
            c.gridx = 2;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            panel.add(player2Label, c);

            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            panel.add(timeElapsedLabel, c);

            c.gridy = 2;
            panel.add(timePlayedLabel, c);

            historyPanels.add(panel);
            historyContainer.add(historyPanels.get(i));
        }

        layout.setVgap(35);
        historyWindow.setSize(300, 500);
        historyWindow.add(jScrollPane);
        historyWindow.setVisible(true);
    }

    /**
     * Method which is called when a player makes a move, to update the currentPlayerName label within the JMenuBar
     * with the appropriate player's name.
     */
    public void updatePlayerLabel(){
        if(gameBoard.getPlayer() == 1){
            currentPlayerName.setText(gameBoard.getPlayer1Name());
        } else if (gameBoard.getPlayer() == 2){
            currentPlayerName.setText(gameBoard.getPlayer2Name());
        }
    }
    //game stuff

    /**
     * Method which is called when a game ends, which stops the game timer, displays a win/draw message and cleans up
     * the UI.
     */
    public void gameEnded() {
        currentPlayerName.setVisible(false);

        timer.stopTimer();
        gameBoard.setTimeElapsed(timer.getTimeElapsed());
        if(gameBoard.getWinner() == 1 || gameBoard.getWinner() == 2){
            String winnerName = "";
            if(gameBoard.getWinner() == 1){
                winnerName = gameBoard.getPlayer1Name();
            } else if (gameBoard.getWinner() == 2){
                winnerName = gameBoard.getPlayer2Name();
            }
            JOptionPane.showMessageDialog(null, winnerName + " wins!", "Victory", JOptionPane.INFORMATION_MESSAGE );
        } else if (gameBoard.getWinner() == 3){
            JOptionPane.showMessageDialog(null, "Draw, you both suck!", "Draw", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Descriptive error message", "Error", JOptionPane.ERROR_MESSAGE);
        }
        try{
            saveToHistory();
        } catch (IOException e) {
            e.printStackTrace();
        }

        createStartArea();
        //need to poke main window again to redraw it I guess
        setSize(500, 500 + getJMenuBar().getHeight());
    }

    //listeners

    /**
     * Listener method which handles the game menu.
     */
    public void actionPerformed(ActionEvent e) {
        String menuName;
        menuName = e.getActionCommand();

        switch (menuName) {
            case "New Game":
                createSettingsMenu();
                settingsMenu.setVisible(true);
                break;
            case "Load Game":
                try {
                    loadGame();
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                break;
            case "Save Game":
                if(gameBoard!=null){
                    try {
                        saveGame();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "You can't save a game when you don't have a game open!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            case "View History":
                try {
                    loadHistory();
                } catch (EOFException ex){
                    //nothing to do here - works as intended?
                } catch (IOException | ClassNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "Game history file not found. Have you finished any games yet?", "Error", JOptionPane.ERROR_MESSAGE);
                }
        }
    }

    /**
     * Listener method which handles when a player clicks on any of the game tiles.
     * It gets the name of the tile which was clicked which contains the index of the column the tile is in and
     * passes that into GameBoard.addTile. After a tile is added, it checks if there is a winner, if so, it calls gameEnded().
     * Otherwise, it checks if the player has opted to play vs the AI, and if so it calls the GameBoard.doAIMove() method.
     * @param e the event which activated the mouseClicked() listener.
     */
    public void mouseClicked(MouseEvent e) {
        JLabel buttonClicked = (JLabel) e.getSource();
        int colClicked = Integer.parseInt(buttonClicked.getName());
        if(gameBoard.getLowestAvailableTile(colClicked) >= 0){
            gameBoard.addTile(colClicked);
            if(gameBoard.checkForWinner() != 0){
                gameEnded();
            }
            if(gameBoard!=null){
                if(!gameBoard.getAIToggle()){
                    gameBoard.switchPlayer();
                    updatePlayerLabel();
                } else {
                    gameBoard.doAIMove();
                    if(gameBoard.checkForWinner() != 0){
                        gameEnded();
                    } else {
                        gameBoard.switchPlayer();
                        updatePlayerLabel();
                    }
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    /**
     *  Listener method which handles when a player hovers over any of the game tiles.
     *  It gets the name of the tile which was hovered which contains the index of the column the tile is in and
     *  passes that into the GameBoard.highlightColumn() method, which changes the background of all the tiles within
     *  the column to "highlight" it.
     */
    public void mouseEntered(MouseEvent e) {
        JLabel buttonEntered = (JLabel) e.getSource();
        int colEntered = Integer.parseInt(buttonEntered.getName());
        gameBoard.highlightColumn(colEntered);
    }

    /**
     *  Listener method which handles when a player hovers out any of the game tiles.
     *  It gets the name of the tile which was hovered out of, which contains the index of the column the tile is in and
     *  passes that into the GameBoard.dehighlightColumn() method, which changes the background of all the tiles within
     *  the column back to their regular colour, white.
     */
    public void mouseExited(MouseEvent e) {
        JLabel buttonEntered = (JLabel) e.getSource();
        int colEntered = Integer.parseInt(buttonEntered.getName());
        gameBoard.dehighlightColumn(colEntered);
    }

    //menu button handlers

    /**
     * Listener method which handles the Start Game button within the settings menu and the start game panel (which is
     * displayed when there's no active game) - the former calling the createGameArea() method, with the latter calling
     * the createSettingsMenu() method.
     */
    private class ButtonEventHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == settingsStartButton){
                    createGameArea();
            } else if (e.getSource() == mainStartButton){
            createSettingsMenu();
            settingsMenu.setVisible(true);
            }
        }
    }
}
