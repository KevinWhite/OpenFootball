import java.applet.Applet;
import java.awt.*;
import java.awt.Graphics;
import java.awt.event.*;
import java.io.*;
import java.net.*;


public class GameViewer extends Applet implements Runnable{

    String            title;
    Thread            thread;
    Font              font;
    Font              largeFont;
    Font              endZoneFont;
    URL               gameDataFile;
    URLConnection     connection;
    InputStreamReader inputStream;
    BufferedReader    bufferedReader;
    String            lineOfData;
    StringBuffer      stringBuffer;
    String            homeTeam;
    String            awayTeam;
    int               snapClock;
    int               playTimer;
    Image             offscreenImage;
    Graphics          offscreen;
    String            tickerText;
    int               tickerTextLength;
    String            ticker;
    Color             endZoneColour;
    Color             endZoneNameColour;
    String            endZoneName;

    int               tickerStartPos;
    int               tickerEndPos;
    int               maxTickerLength;
    int               homeTeamTimeOuts;
    int               awayTeamTimeOuts;
    int               currentQuarter;
    int               homeTeamScoring[];
    int               awayTeamScoring[];
    int               currentDown;
    int               distance;
    int               ballOn;
    int               drawBallOn;
    int               minutesLeft;
    int               secondsLeft;
    int               boxTextLength;
    int               gridLines;
    int               downLines;
    int               yPos;
    int               yMinPos;
    int               yMaxPos;
    int               currentLine;
    int               topLine;
    int               endZoneNameXPos;
    int               animateYards;
    int               yardsGained;
    int               sleepCounter;
    int               playDelayCounter;
    int               playDelay[];
    int               offensivePlayer[];
    int               offensivePlayerXPos[];
    int               offensivePlayerYPos[];


    String            miniTronBoxText[];
    String            tickerHomeTeam[];
    String            tickerAwayTeam[];
    String            tickerHomeScore[];
    String            tickerAwayScore[];
    String            playDescriptionBoxText[];
    String            homeTeamStatsBoxText[];
    String            awayTeamStatsBoxText[];

    boolean           homeTeamPossession;
    boolean           kickOff;
    boolean           extraPoint;
    boolean           miniTronFlash;
    boolean           flashTimer;
    boolean           pitchHomeTeamPossession;
    boolean           startOfPlay;
    boolean           endOfPlay;
    boolean           playDescriptionLine[];

    public static void main(String[] args) {

        GameViewerWindow gameViewerWindow = new GameViewerWindow("Game Viewer");
        gameViewerWindow.setSize(800,600);
        gameViewerWindow.show();
    }

    public void init() {

        System.out.println("Game Viewer v0.0.044 \u00A9 Clubpit 2001");

        setLayout(null);

        setBackground(Color.darkGray);

        title                  = "Game Viewer v0.0.044 \u00A9 ClubPit 2001";
        thread                 = null;
        font                   = new Font("Courier" , Font.PLAIN, 12 );
        largeFont              = new Font("Courier" , Font.BOLD,22 );
        endZoneFont            = new Font("Courier" , Font.BOLD,70 );
        gameDataFile           = null;
        connection             = null;
        inputStream            = null;
        bufferedReader         = null;
        lineOfData             = null;
        stringBuffer           = new StringBuffer();
        snapClock              = 5;
        playTimer              = 0;
        offscreenImage         = createImage(size().width,size().height);
        offscreen              = offscreenImage.getGraphics();
        tickerText             = null;
        tickerTextLength       = 0;
        tickerStartPos         = -1;
        tickerEndPos           = 107;
        maxTickerLength        = 107;
        homeTeamTimeOuts       = 3;
        awayTeamTimeOuts       = 3;
        currentQuarter         = 1;
        homeTeamScoring        = new int[6];
        awayTeamScoring        = new int[6];
        playDelayCounter       = 0;
        playDelay              = new int[6];
        offensivePlayer        = new int[30];
        offensivePlayerXPos    = new int[30];
        offensivePlayerYPos    = new int[30];
        ticker                 = null;
        currentDown            = 1;
        distance               = 0;
        ballOn                 = 35;
        drawBallOn             = 0;
        minutesLeft            = 15;
        secondsLeft            = 0;
        boxTextLength          = 27;
        gridLines              = 0;
        downLines              = 10;
        yPos                   = 0;
        yMaxPos                = 0;
        currentLine            = 0;
        endZoneNameXPos        = 10;
        animateYards           = 0;
        sleepCounter          = 0;
        endZoneColour          = new Color(255,204,102);
        endZoneNameColour      = new Color(0,0,0);
        endZoneName            = "J A G U A R S";

        miniTronBoxText        = new String[6];
        tickerHomeTeam         = new String[12];
        tickerAwayTeam         = new String[12];
        tickerHomeScore        = new String[12];
        tickerAwayScore        = new String[12];
        playDescriptionBoxText = new String[7];
        homeTeamStatsBoxText   = new String[7];
        awayTeamStatsBoxText   = new String[7];

        homeTeamPossession      = false;
        kickOff                 = true;
        extraPoint              = false;
        miniTronFlash           = false;
        flashTimer              = false;
        pitchHomeTeamPossession = false;
        startOfPlay             = false;
        endOfPlay               = true;
        playDescriptionLine     = new boolean[7];

        //Test Code

        homeTeam          = "Jacksonville Jaguars";
        awayTeam          = "Cincinnati Bengals";

        homeTeamTimeOuts   = 1;
        currentQuarter     = 2;
        homeTeamScoring[0] = 3;
        homeTeamScoring[5] = 3;
        playDelay[0]       = 4;
        playDelay[1]       = 2;
        playDelay[2]       = 0;
        playDelay[3]       = 0;
        playDelay[4]       = 0;
        playDelay[5]       = 0;
        currentDown        = 3;
        distance           = 1;
        ballOn             = 99;
        minutesLeft        = 1;
        secondsLeft        = 15;
        yardsGained        = 99;

        kickOff            = false;

        miniTronBoxText[0] = "*Miami  10";
        miniTronBoxText[1] = " Denver 21";
        miniTronBoxText[2] = " 2nd Quarter";
        miniTronBoxText[3] = " 10:13 Remaining";
        miniTronBoxText[4] = " 3rd & 4";
        miniTronBoxText[5] = " RB Brooks 5yd run";

        offensivePlayer[0] = 71;
        offensivePlayer[1] = 72;
        offensivePlayer[2] = 73;
        offensivePlayer[3] = 74;
        offensivePlayer[4] = 75;
        offensivePlayer[5] = 88;
        offensivePlayer[6] = 81;
        offensivePlayer[7] = 82;
        offensivePlayer[8] = 21;
        offensivePlayer[9] = 44;
        offensivePlayer[10] = 12;

        offensivePlayerXPos[0] = 12;
        offensivePlayerXPos[1] = 13;
        offensivePlayerXPos[2] = 14;
        offensivePlayerXPos[3] = 15;
        offensivePlayerXPos[4] = 16;
        offensivePlayerXPos[5] = 11;
        offensivePlayerXPos[6] = 03;
        offensivePlayerXPos[7] = 24;
        offensivePlayerXPos[8] = 13;
        offensivePlayerXPos[9] = 15;
        offensivePlayerXPos[10] = 14;

        offensivePlayerYPos[0] = 103;
        offensivePlayerYPos[1] = 103;
        offensivePlayerYPos[2] = 103;
        offensivePlayerYPos[3] = 103;
        offensivePlayerYPos[4] = 103;
        offensivePlayerYPos[5] = 103;
        offensivePlayerYPos[6] = 102;
        offensivePlayerYPos[7] = 103;
        offensivePlayerYPos[8] = 100;
        offensivePlayerYPos[9] = 100;
        offensivePlayerYPos[10] = 102;

        tickerHomeTeam[0] = "Tennessee Titans";
        tickerHomeTeam[1] = "Pittsburgh Steelers";
        tickerHomeTeam[2] = "Jacksonville Jaguars";
        tickerHomeTeam[3] = "Miami Dolphins";
        tickerHomeTeam[4] = "Buffalo Bills";
        tickerHomeTeam[5] = "Oakland Raiders";
        tickerHomeTeam[6] = "Washington Redskins";
        tickerHomeTeam[7] = "Arizona Cardinals";
        tickerHomeTeam[8] = "New Orleans Saints";
        tickerHomeTeam[9] = "Green Bay Packers";
        tickerHomeTeam[10] = "Carolina Panthers";
        tickerHomeTeam[11] = "Dallas Cowboys";

        tickerAwayTeam[0] = "Seattle Seahawks";
        tickerAwayTeam[1] = "Indianapolis Colts";
        tickerAwayTeam[2] = "Cincinnati Bengals";
        tickerAwayTeam[3] = "Denver Broncos";
        tickerAwayTeam[4] = "New England Patriots";
        tickerAwayTeam[5] = "Baltimore Ravens";
        tickerAwayTeam[6] = "Tampa Bay Buccaneers";
        tickerAwayTeam[7] = "Philadelphia Eagles";
        tickerAwayTeam[8] = "Detroit Lions";
        tickerAwayTeam[9] = "St Louis Rams";
        tickerAwayTeam[10] = "San Francisco 49ers";
        tickerAwayTeam[11] = "Minnesota Vikings";

        tickerHomeScore[0] = "34";
        tickerHomeScore[1] = "30";
        tickerHomeScore[2] = "26";
        tickerHomeScore[3] = "10";
        tickerHomeScore[4] = "00";
        tickerHomeScore[5] = "24";
        tickerHomeScore[6] = "16";
        tickerHomeScore[7] = "31";
        tickerHomeScore[8] = "36";
        tickerHomeScore[9] = "14";
        tickerHomeScore[10] = "29";
        tickerHomeScore[11] = "42";

        tickerAwayScore[0] = "28";
        tickerAwayScore[1] = "27";
        tickerAwayScore[2] = "08";
        tickerAwayScore[3] = "30";
        tickerAwayScore[4] = "29";
        tickerAwayScore[5] = "17";
        tickerAwayScore[6] = "28";
        tickerAwayScore[7] = "13";
        tickerAwayScore[8] = "00";
        tickerAwayScore[9] = "29";
        tickerAwayScore[10] = "06";
        tickerAwayScore[11] = "06";


        tickerText = tickerHomeTeam[0]  + " " +
                     tickerHomeScore[0] + " " +
                     tickerAwayTeam[0]  + " " +
                     tickerAwayScore[0] + "....." +
                     tickerHomeTeam[1]  + " " +
                     tickerHomeScore[1] + " " +
                     tickerAwayTeam[1]  + " " +
                     tickerAwayScore[1] + "....." +
                     tickerHomeTeam[2]  + " " +
                     tickerHomeScore[2] + " " +
                     tickerAwayTeam[2]  + " " +
                     tickerAwayScore[2] + "....." +
                     tickerHomeTeam[3]  + " " +
                     tickerHomeScore[3] + " " +
                     tickerAwayTeam[3]  + " " +
                     tickerAwayScore[3] + "....." +
                     tickerHomeTeam[4]  + " " +
                     tickerHomeScore[4] + " " +
                     tickerAwayTeam[4]  + " " +
                     tickerAwayScore[4] + "....." +
                     tickerHomeTeam[5]  + " " +
                     tickerHomeScore[5] + " " +
                     tickerAwayTeam[5]  + " " +
                     tickerAwayScore[5] + "....." +
                     tickerHomeTeam[6]  + " " +
                     tickerHomeScore[6] + " " +
                     tickerAwayTeam[6]  + " " +
                     tickerAwayScore[6] + "....." +
                     tickerHomeTeam[7]  + " " +
                     tickerHomeScore[7] + " " +
                     tickerAwayTeam[7]  + " " +
                     tickerAwayScore[7] + "....." +
                     tickerHomeTeam[8]  + " " +
                     tickerHomeScore[8] + " " +
                     tickerAwayTeam[8]  + " " +
                     tickerAwayScore[8] + "....." +
                     tickerHomeTeam[9]  + " " +
                     tickerHomeScore[9] + " " +
                     tickerAwayTeam[9]  + " " +
                     tickerAwayScore[9] + "....." +
                     tickerHomeTeam[10]  + " " +
                     tickerHomeScore[10] + " " +
                     tickerAwayTeam[10]  + " " +
                     tickerAwayScore[10] + "....." +
                     tickerHomeTeam[11]  + " " +
                     tickerHomeScore[11] + " " +
                     tickerAwayTeam[11]  + " " +
                     tickerAwayScore[11] + ".....";

        tickerTextLength = tickerText.length();

        //End of Test Code

        Checkbox autoPlayBox = new Checkbox("AutoPlay");
        autoPlayBox.reshape(610,532,60,20);
        add(autoPlayBox);

        Button genericButton = new Button("Play");
        genericButton.reshape(680,533,50,19);
        add(genericButton);

//        try {
//           gameDataFile = new URL("http://192.168.1.1/Game.dat");
//             gameDataFile = new URL("http://www.members.aol.com/majormainframe/Game.dat");
//        }
//        catch (MalformedURLException e) {
//             System.out.println("File not found at : " + gameDataFile );
//        }
//
//        try {
//            connection = this.gameDataFile.openConnection();
//            connection.connect();
//            inputStream = new InputStreamReader(connection.getInputStream());
//            bufferedReader = new BufferedReader(inputStream);
//        }
//        catch (IOException e) {
//            System.out.println("File Error : " + e.getMessage());
//        }
//
    }

    public void start() {

        if (thread == null) {
           thread = new Thread(this);
           thread.start();
        }

    }

    public void run() {

        Thread snapThread = Thread.currentThread();

        while ( thread == snapThread) {
            repaint();
            try {

                Thread.sleep(150);

                if ( sleepCounter == 4 )
                   {
                   if ( tickerStartPos < tickerTextLength )
                      tickerStartPos++;
                   else
                      tickerStartPos = 0;

                   if ( tickerEndPos == tickerTextLength )
                      tickerEndPos = 0;
                   else
                      tickerEndPos++;

                   if ( tickerEndPos > tickerStartPos )
                      ticker = tickerText.substring(tickerStartPos,tickerEndPos);
                   else
                      ticker = tickerText.substring(tickerStartPos,tickerTextLength) +
                               tickerText.substring(0,tickerEndPos);
                   if ( snapClock > 0 )
                      snapClock--;
                   sleepCounter = 0;
                   }
                else
                   sleepCounter++;

                if ( snapClock == 30 )
                   {
                   miniTronFlash      = false;
                   miniTronBoxText[0] = "*Miami  10";
                   miniTronBoxText[1] = " Denver 21";
                   miniTronBoxText[2] = " 2nd Quarter";
                   miniTronBoxText[3] = " 10:13 Remaining";
                   miniTronBoxText[4] = " 3rd & 4";
                   miniTronBoxText[5] = " RB Brooks 5yd run";
                   }

                if ( snapClock == 3 )
                   {
                   homeTeamScoring[1] = 0;
                   homeTeamScoring[5] = 3;
                   homeTeamPossession = false;
                   extraPoint         = false;
                   currentDown        = 3;
                   distance           = 1;
                   ballOn             = 99;
                   drawBallOn         = 99;
                   miniTronFlash      = false;
                   minutesLeft        = 1;
                   secondsLeft        = 15;
                   offscreen.setColor(Color.black);
                   offscreen.drawLine(16,71,195,71);
                   miniTronBoxText[0] = null;
                   miniTronBoxText[1] = null;
                   miniTronBoxText[2] = null;
                   miniTronBoxText[3] = null;
                   miniTronBoxText[4] = null;
                   miniTronBoxText[5] = null;
                   playDescriptionBoxText[0] = null;
                   playDescriptionBoxText[1] = null;
                   playDescriptionBoxText[2] = null;
                   playDescriptionBoxText[3] = null;
                   playDescriptionBoxText[4] = null;
                   playDescriptionBoxText[5] = null;
                   playDescriptionBoxText[6] = null;
                   homeTeamStatsBoxText[0]   = null;
                   homeTeamStatsBoxText[1]   = null;
                   homeTeamStatsBoxText[2]   = null;
                   homeTeamStatsBoxText[3]   = null;
                   homeTeamStatsBoxText[4]   = null;
                   homeTeamStatsBoxText[5]   = null;
                   homeTeamStatsBoxText[6]   = null;
                   awayTeamStatsBoxText[0]   = null;
                   awayTeamStatsBoxText[1]   = null;
                   awayTeamStatsBoxText[2]   = null;
                   awayTeamStatsBoxText[3]   = null;
                   awayTeamStatsBoxText[4]   = null;
                   awayTeamStatsBoxText[5]   = null;
                   awayTeamStatsBoxText[6]   = null;
                   playDescriptionLine[0]    = false;
                   playDescriptionLine[1]    = false;
                   playDescriptionLine[2]    = false;
                   playDescriptionLine[3]    = false;
                   playDescriptionLine[4]    = false;
                   playDescriptionLine[5]    = false;
                   playDescriptionLine[6]    = false;
                   endOfPlay                 = false;
                   animateYards              = 0;
                   yardsGained               = 99;
                   offensivePlayerXPos[0] = 12;
                   offensivePlayerXPos[1] = 13;
                   offensivePlayerXPos[2] = 14;
                   offensivePlayerXPos[3] = 15;
                   offensivePlayerXPos[4] = 16;
                   offensivePlayerXPos[5] = 11;
                   offensivePlayerXPos[6] = 03;
                   offensivePlayerXPos[7] = 24;
                   offensivePlayerXPos[8] = 13;
                   offensivePlayerXPos[9] = 15;
                   offensivePlayerXPos[10] = 14;
                   offensivePlayerYPos[0] = 103;
                   offensivePlayerYPos[1] = 103;
                   offensivePlayerYPos[2] = 103;
                   offensivePlayerYPos[3] = 103;
                   offensivePlayerYPos[4] = 103;
                   offensivePlayerYPos[5] = 103;
                   offensivePlayerYPos[6] = 102;
                   offensivePlayerYPos[7] = 103;
                   offensivePlayerYPos[8] = 100;
                   offensivePlayerYPos[9] = 100;
                   offensivePlayerYPos[10] = 102;
                   offensivePlayer[0] = 71;
                   offensivePlayer[1] = 72;
                   offensivePlayer[2] = 73;
                   offensivePlayer[3] = 74;
                   offensivePlayer[4] = 75;
                   offensivePlayer[5] = 88;
                   offensivePlayer[6] = 81;
                   offensivePlayer[7] = 82;
                   offensivePlayer[8] = 21;
                   offensivePlayer[9] = 44;
                   offensivePlayer[10] = 12;
                   }

                if ( snapClock == 0 )
                   {
                   snapClock = 0;
                   startOfPlay = true;
                   }

                if ( startOfPlay == true )
                   {
                   if ( playDescriptionLine[0] == false )
                      {
                      playDescriptionBoxText[0] = "QB Shuler drops back";
                      playDescriptionLine[0] = true;
                      }
                   else
                      if ( playDescriptionLine[0] == true && playDelayCounter == playDelay[0] && playDescriptionLine[1] == false )
                         {
                         playDescriptionBoxText[1] = "Throws over the middle";
                         playDescriptionLine[1] = true;
                         playDelayCounter = 0;
                         }
                      else
                         if ( playDescriptionLine[1] == true && playDelayCounter == playDelay[1] && playDescriptionLine[2] == false )
                            {
                            playDescriptionBoxText[2] = "INTERCEPTED!";
                            playDescriptionLine[2] = true;
                            playDelayCounter = 0;
                            }
                         else
                            if ( playDescriptionLine[2] == true && playDelayCounter == playDelay[2] && playDescriptionLine[3] == false )
                               {
                               playDescriptionBoxText[3] = "by CB Poole at the GoalLine";
                               playDescriptionLine[3] = true;
                               playDelayCounter = 0;
                               }
                            else
                               if ( playDescriptionLine[3] == true && playDelayCounter == playDelay[3] && playDescriptionLine[4] == false )
                                  {
                                  animateYards += 1;
                                  ballOn -= 1;
                                  if ( animateYards < yardsGained )
                                      {
                                      playDescriptionBoxText[4] = "Returned for " + animateYards + "yds";
                                      }
                                  else
                                      {
                                      playDescriptionBoxText[4] = "Returned for " + yardsGained + "yds";
                                      playDescriptionLine[4] = true;
                                      playDelayCounter = 0;
                                      }
                                  }
                               else
                                  if ( playDescriptionLine[4] == true && playDelayCounter == playDelay[4] && playDescriptionLine[5] == false )
                                     {
                                     playDescriptionBoxText[5] = "!!!TOUCHDOWN!!!";
                                     homeTeamScoring[1] += 6;
                                     homeTeamScoring[5] += 6;
                                     miniTronFlash = true;
                                     miniTronBoxText[0] = "*************";
                                     miniTronBoxText[1] = "* TOUCHDOWN *";
                                     miniTronBoxText[2] = "*************";
                                     extraPoint = true;
                                     currentDown = -1;
                                     distance = -1;
                                     ballOn = 3;
                                     minutesLeft = 1;
                                     secondsLeft = 1;
                                     playDescriptionLine[5] = true;
                                     startOfPlay = false;
                                     endOfPlay = true;
                                     homeTeamPossession = true;
                                     playDelayCounter = 0;
                                     offensivePlayer[0] = 71;
                                     offensivePlayer[1] = 72;
                                     offensivePlayer[2] = 73;
                                     offensivePlayer[3] = 74;
                                     offensivePlayer[4] = 75;
                                     offensivePlayer[5] = 76;
                                     offensivePlayer[6] = 77;
                                     offensivePlayer[7] = 88;
                                     offensivePlayer[8] = 89;
                                     offensivePlayer[9] = 2;
                                     offensivePlayer[10] = 1;
                                     offensivePlayerXPos[0] = 11;
                                     offensivePlayerXPos[1] = 12;
                                     offensivePlayerXPos[2] = 13;
                                     offensivePlayerXPos[3] = 14;
                                     offensivePlayerXPos[4] = 15;
                                     offensivePlayerXPos[5] = 16;
                                     offensivePlayerXPos[6] = 17;
                                     offensivePlayerXPos[7] = 10;
                                     offensivePlayerXPos[8] = 18;
                                     offensivePlayerXPos[9] = 15;
                                     offensivePlayerXPos[10] = 13;
                                     offensivePlayerYPos[0] = 9;
                                     offensivePlayerYPos[1] = 9;
                                     offensivePlayerYPos[2] = 9;
                                     offensivePlayerYPos[3] = 9;
                                     offensivePlayerYPos[4] = 9;
                                     offensivePlayerYPos[5] = 9;
                                     offensivePlayerYPos[6] = 9;
                                     offensivePlayerYPos[7] = 10;
                                     offensivePlayerYPos[8] = 10;
                                     offensivePlayerYPos[9] = 12;
                                     offensivePlayerYPos[10] = 14;
                                     }
                                  else
                                     if ( playDescriptionLine[5] == true && playDelayCounter == playDelay[5] && playDescriptionLine[6] == false )
                                        {
                                        playDescriptionBoxText[6] = "";
                                        playDescriptionLine[6] = true;
                                        playDelayCounter = 0;
                                        }
                                     else
                                        playDelayCounter++;

                if ( endOfPlay == true )
                      {
                      playTimer = 0;
                      homeTeamStatsBoxText[0]   = "CB Tyrone Poole";
                      homeTeamStatsBoxText[1]   = "---------------";
                      homeTeamStatsBoxText[2]   = "";
                      homeTeamStatsBoxText[3]   = "      Ints Tck";
                      homeTeamStatsBoxText[4]   = "";
                      homeTeamStatsBoxText[5]   = "Today    1   3";
                      homeTeamStatsBoxText[6]   = "Season   5  52";

                      awayTeamStatsBoxText[0]   = "QB Heath Shuler";
                      awayTeamStatsBoxText[1]   = "---------------";
                      awayTeamStatsBoxText[2]   = "";
                      awayTeamStatsBoxText[3]   = "       Att Com  Yds Int Td";
                      awayTeamStatsBoxText[4]   = "";
                      awayTeamStatsBoxText[5]   = "Today   10   5   79   1  1";
                      awayTeamStatsBoxText[6]   = "Season 265 113  592   5 12";
                      snapClock = 35;
                      }
                }

            } catch (InterruptedException e) { }
        }

        try {
            lineOfData = bufferedReader.readLine();
            //while ((line = data.readLine()) != null) {
                stringBuffer.append(lineOfData);
            //}
        }
        catch (IOException e) {
            System.out.println("File Error : " + e.getMessage());
        }
        repaint();
    }

    public void paint(Graphics screen) {

        offscreen.setFont(font);
        offscreen.setColor(Color.darkGray);

        // Main Panel
        offscreen.fillRect(0,0,size().width,size().height);

        offscreen.setColor(Color.gray);

        // Scoreboard Panel
        offscreen.fillRoundRect(2,20,786,138,20,20);

        // Description Box's Panel
        offscreen.fillRoundRect(570,160,218,398,20,20);

        offscreen.setColor(Color.white);

        // Pitch Panel
        offscreen.fillRoundRect(2,160,565,398,20,20);

        offscreen.setColor(Color.darkGray);

        // MiniTron Border
        offscreen.fillRoundRect(6,39,180,85,20,20);

        // HomeTeam Border
        offscreen.fillRoundRect(218,39,240,23,20,20);

        // AwayTeam Border
        offscreen.fillRoundRect(218,69,240,23,20,20);

        // HomeTeam Possession Border
        offscreen.fillRoundRect(460,39,23,23,23,23);

        // AwayTeam Possession Border
        offscreen.fillRoundRect(460,69,23,23,23,23);

        // HomeTeam Timeouts Border
        offscreen.fillRoundRect(489,39,40,23,20,20);

        // AwayTeam Timeouts Border
        offscreen.fillRoundRect(489,69,40,23,20,20);

        // HomeTeam 1st Quarter Border
        offscreen.fillRoundRect(535,39,40,23,20,20);

        // AwayTeam 1st Quarter Border
        offscreen.fillRoundRect(535,69,40,23,20,20);

        // HomeTeam 2nd Quarter Border
        offscreen.fillRoundRect(577,39,40,23,20,20);

        // AwayTeam 2nd Quarter Border
        offscreen.fillRoundRect(577,69,40,23,20,20);

        // HomeTeam 3rd Quarter Border
        offscreen.fillRoundRect(619,39,40,23,20,20);

        // AwayTeam 3rd Quarter Border
        offscreen.fillRoundRect(619,69,40,23,20,20);

        // HomeTeam 4th Quarter Border
        offscreen.fillRoundRect(661,39,40,23,20,20);

        // AwayTeam 4th Quarter Border
        offscreen.fillRoundRect(661,69,40,23,20,20);

        // HomeTeam OverTime Border
        offscreen.fillRoundRect(703,39,40,23,20,20);

        // AwayTeam OverTime Border
        offscreen.fillRoundRect(703,69,40,23,20,20);

        // HomeTeam Total Border
        offscreen.fillRoundRect(745,39,40,23,20,20);

        // AwayTeam Total Border
        offscreen.fillRoundRect(745,69,40,23,20,20);

        // Down Border
        offscreen.fillRoundRect(218,99,40,23,20,20);

        // Distance Border
        offscreen.fillRoundRect(330,99,40,23,20,20);

        // BallOn Border
        offscreen.fillRoundRect(430,99,40,23,20,20);

        // Quarter Border
        offscreen.fillRoundRect(530,99,40,23,20,20);

        // TimeLeft Border
        offscreen.fillRoundRect(645,99,55,23,20,20);

        // SnapClock Border
        offscreen.fillRoundRect(745,99,40,23,20,20);

        // Ticker Border
        offscreen.fillRoundRect(6,129,779,24,20,20);

        // Play Description Border
        offscreen.fillRoundRect(575,180,208,101,20,20);

        // Home Team Stats Border
        offscreen.fillRoundRect(575,300,208,101,20,20);

        // Away Team Stats Border
        offscreen.fillRoundRect(575,420,208,101,20,20);

        // Control Box
        offscreen.fillRoundRect(575,530,208,24,20,20);

        offscreen.setColor(Color.black);

        // MiniTron Box
        offscreen.fillRoundRect(11,45,170,75,20,20);

        // HomeTeam Box
        offscreen.fillRoundRect(223,44,230,13,20,20);

        // AwayTeam Box
        offscreen.fillRoundRect(223,74,230,13,20,20);

        // HomeTeam Possession Box
        offscreen.fillRoundRect(464,43,15,15,15,15);

        // AwayTeam Possession Box
        offscreen.fillRoundRect(464,73,15,15,15,15);

        // HomeTeam TimeOuts Box
        offscreen.fillRoundRect(494,44,30,13,20,20);

        // AwayTeam TimeOuts Box
        offscreen.fillRoundRect(494,74,30,13,20,20);

        // HomeTeam 1st Quarter Box
        offscreen.fillRoundRect(540,44,30,13,20,20);

        // AwayTeam 1st Quarter Box
        offscreen.fillRoundRect(540,74,30,13,20,20);

        // HomeTeam 2nd Quarter Box
        offscreen.fillRoundRect(582,44,30,13,20,20);

        // AwayTeam 2nd Quarter Box
        offscreen.fillRoundRect(582,74,30,13,20,20);

        // HomeTeam 3rd Quarter Box
        offscreen.fillRoundRect(624,44,30,13,20,20);

        // AwayTeam 3rd Quarter Box
        offscreen.fillRoundRect(624,74,30,13,20,20);

        // HomeTeam 4th Quarter Box
        offscreen.fillRoundRect(666,44,30,13,20,20);

        // AwayTeam 4th QuarterBox
        offscreen.fillRoundRect(666,74,30,13,20,20);

        // HomeTeam OverTime Box
        offscreen.fillRoundRect(708,44,30,13,20,20);

        // AwayTeam OverTime Box
        offscreen.fillRoundRect(708,74,30,13,20,20);

        // HomeTeam Total Box
        offscreen.fillRoundRect(750,44,30,13,20,20);

        // AwayTeam Total Box
        offscreen.fillRoundRect(750,74,30,13,20,20);

        // Down Box
        offscreen.fillRoundRect(223,104,30,13,20,20);

        // Distance Box
        offscreen.fillRoundRect(335,104,30,13,20,20);

        // BallOn Box
        offscreen.fillRoundRect(435,104,30,13,20,20);

        // Quarter Box
        offscreen.fillRoundRect(535,104,30,13,20,20);

        // TimeLeft
        offscreen.fillRoundRect(650,104,45,13,20,20);

        // SnapClock
        offscreen.fillRoundRect(750,104,30,13,20,20);

        // Ticker Box
        offscreen.fillRoundRect(11,134,769,14,20,20);

        // Play Description Box
        offscreen.fillRoundRect(580,185,198,91,20,20);

        // Home Team Stats Box
        offscreen.fillRoundRect(580,305,198,91,20,20);

        // Away Team Stats Box
        offscreen.fillRoundRect(580,425,198,91,20,20);

        // Box Titles
        offscreen.drawString("MiniTron\u2122",70,35);
        offscreen.drawString("Teams",320,35);
        offscreen.drawString("Poss",455,35);
        offscreen.drawString("T.O.L",490,35);
        offscreen.drawString("1st",545,35);
        offscreen.drawString("2nd",587,35);
        offscreen.drawString("3rd",629,35);
        offscreen.drawString("4th",671,35);
        offscreen.drawString("OT",716,35);
        offscreen.drawString("Total",748,35);
        offscreen.drawString("Home",188,54);
        offscreen.drawString("Away",188,84);
        offscreen.drawString("Down",188,114);
        offscreen.drawString("Distance",272,114);
        offscreen.drawString("Ball On",379,114);
        offscreen.drawString("Quarter",479,114);
        offscreen.drawString("Time Left",579,114);
        offscreen.drawString("Snap",715,114);
        offscreen.drawString("Play Description",625,175);
        offscreen.drawString("Home Team Statistics",615,295);
        offscreen.drawString("Away Team Statistics",615,415);

        offscreen.setColor(Color.green);

        // Pitch Box
        offscreen.fillRoundRect(7,165,555,388,20,20);

        // Draw Title
        offscreen.drawString(title,260,14);

        // Draw Ticker

        if ( ticker != null )
           offscreen.drawString(ticker,20,144);

        offscreen.setColor(Color.yellow);

        // Draw Contents of MiniTron

        if ( miniTronBoxText[0] != null )
           if ( miniTronFlash )
              {
              offscreen.setColor(Color.black);
              offscreen.drawLine(13,71,178,71);
              offscreen.setFont(largeFont);
              if ( flashTimer )
                 offscreen.setColor(Color.green);
              else
                 offscreen.setColor(Color.yellow);
              offscreen.drawString(miniTronBoxText[0],11,65);
              offscreen.drawString(miniTronBoxText[1],11,90);
              offscreen.drawString(miniTronBoxText[2],11,115);
              offscreen.setFont(font);
              offscreen.setColor(Color.yellow);
              if ( flashTimer )
                 flashTimer = false;
              else
                 flashTimer = true;
              }
           else
              {
              offscreen.drawString(miniTronBoxText[0],15,55);
              offscreen.drawString(miniTronBoxText[1],15,68);
              if ( miniTronBoxText[0] != null )
                 offscreen.drawLine(13,71,178,71);
              offscreen.drawString(miniTronBoxText[2],15,82);
              offscreen.drawString(miniTronBoxText[3],15,94);
              offscreen.drawString(miniTronBoxText[4],15,106);
              offscreen.drawString(miniTronBoxText[5],15,117);
              }

        // Draw HomeTeam Contents
        offscreen.drawString(homeTeam,232,54);

        // Draw AwayTeam Contents
        offscreen.drawString(awayTeam,232,84);

        if ( homeTeamPossession )
           // Draw HomeTeam Possession
           offscreen.fillOval(468,47,7,7);
        else
           // Draw AwayTeam Possession
           offscreen.fillOval(468,77,7,7);

        // Draw HomeTeam TimeOuts

        int homeTimeOutsXPos = 496;
        for ( int timeOutCounter = 0 ; timeOutCounter < homeTeamTimeOuts ; timeOutCounter++ )
            {
            offscreen.fillOval(homeTimeOutsXPos,47,7,7);
            homeTimeOutsXPos += 10;
            }

        // Draw Away HomeTeam TimeOuts

        int awayTimeOutsXPos = 496;
        for ( int timeOutCounter = 0 ; timeOutCounter < awayTeamTimeOuts ; timeOutCounter++ )
            {
            offscreen.fillOval(awayTimeOutsXPos,77,7,7);
            awayTimeOutsXPos += 10;
            }

        // Draw HomeTeam 1st Quarter Score

        if ( currentQuarter > 0 )
           if ( homeTeamScoring[0] > 99 )
              offscreen.drawString((String.valueOf(homeTeamScoring[0])),543,55);
           else
               if ( homeTeamScoring[0] > 9 )
                 offscreen.drawString((String.valueOf(homeTeamScoring[0])),550,55);
              else
                 offscreen.drawString((String.valueOf(homeTeamScoring[0])),557,55);

        // Draw AwayTeam 1st Quarter Score

        if ( currentQuarter > 0 )
           if ( awayTeamScoring[0] > 99 )
              offscreen.drawString((String.valueOf(awayTeamScoring[0])),543,85);
           else
              if ( awayTeamScoring[0] > 9 )
                 offscreen.drawString((String.valueOf(awayTeamScoring[0])),550,85);
              else
                 offscreen.drawString((String.valueOf(awayTeamScoring[0])),557,85);

        // Draw HomeTeam 2nd Quarter Score

        if ( currentQuarter > 1 )
           if ( homeTeamScoring[1] > 99 )
              offscreen.drawString((String.valueOf(homeTeamScoring[1])),585,55);
           else
              if ( homeTeamScoring[1] > 9 )
                 offscreen.drawString((String.valueOf(homeTeamScoring[1])),592,55);
              else
                 offscreen.drawString((String.valueOf(homeTeamScoring[1])),599,55);

        // Draw AwayTeam 2nd Quarter Score

        if ( currentQuarter > 1 )
           if ( awayTeamScoring[1] > 99 )
              offscreen.drawString((String.valueOf(awayTeamScoring[1])),585,85);
           else
              if ( awayTeamScoring[1] > 9 )
                 offscreen.drawString((String.valueOf(awayTeamScoring[1])),592,85);
              else
                 offscreen.drawString((String.valueOf(awayTeamScoring[1])),599,85);

        // Draw HomeTeam 3rd Quarter Score

        if ( currentQuarter > 2 )
           if ( homeTeamScoring[2] > 99 )
              offscreen.drawString((String.valueOf(homeTeamScoring[2])),627,55);
           else
              if ( homeTeamScoring[2] > 9 )
                 offscreen.drawString((String.valueOf(homeTeamScoring[2])),634,55);
              else
                 offscreen.drawString((String.valueOf(homeTeamScoring[2])),641,55);

        // Draw AwayTeam 3rd Quarter Score

        if ( currentQuarter > 2 )
           if ( awayTeamScoring[2] > 99 )
              offscreen.drawString((String.valueOf(awayTeamScoring[2])),627,85);
           else
              if ( awayTeamScoring[2] > 9 )
                 offscreen.drawString((String.valueOf(awayTeamScoring[2])),634,85);
              else
                 offscreen.drawString((String.valueOf(awayTeamScoring[2])),641,85);

        // Draw HomeTeam 4th Quarter Score

        if ( currentQuarter > 3 )
           if ( homeTeamScoring[3] > 99 )
              offscreen.drawString((String.valueOf(homeTeamScoring[3])),669,55);
           else
              if ( homeTeamScoring[3] > 9 )
                 offscreen.drawString((String.valueOf(homeTeamScoring[3])),676,55);
              else
                 offscreen.drawString((String.valueOf(homeTeamScoring[3])),683,55);

        // Draw AwayTeam 4th Quarter Score

        if ( currentQuarter > 3 )
           if ( awayTeamScoring[3] > 99 )
              offscreen.drawString((String.valueOf(awayTeamScoring[3])),669,85);
           else
              if ( awayTeamScoring[3] > 9 )
                 offscreen.drawString((String.valueOf(awayTeamScoring[3])),676,85);
              else
                 offscreen.drawString((String.valueOf(awayTeamScoring[3])),683,85);

        // Draw HomeTeam OT Score

        if ( currentQuarter > 4 )
           if ( homeTeamScoring[4] > 99 )
              offscreen.drawString((String.valueOf(homeTeamScoring[4])),711,55);
           else
              if ( homeTeamScoring[4] > 9 )
                 offscreen.drawString((String.valueOf(homeTeamScoring[4])),718,55);
              else
                 offscreen.drawString((String.valueOf(homeTeamScoring[4])),725,55);

        // Draw AwayTeam OT Score

        if ( currentQuarter > 4 )
           if ( awayTeamScoring[4] > 99 )
              offscreen.drawString((String.valueOf(awayTeamScoring[4])),711,85);
           else
              if ( awayTeamScoring[4] > 9 )
                 offscreen.drawString((String.valueOf(awayTeamScoring[4])),718,85);
              else
                offscreen.drawString((String.valueOf(awayTeamScoring[4])),725,85);

        // Draw HomeTeam Total Score

        if ( homeTeamScoring[5] > 99 )
           offscreen.drawString((String.valueOf(homeTeamScoring[5])),753,55);
        else
           if ( homeTeamScoring[5] > 9 )
              offscreen.drawString((String.valueOf(homeTeamScoring[5])),760,55);
           else
              offscreen.drawString((String.valueOf(homeTeamScoring[5])),767,55);

        // Draw AwayTeam Total Score

        if ( awayTeamScoring[5] > 99 )
           offscreen.drawString((String.valueOf(awayTeamScoring[5])),753,85);
        else
           if ( awayTeamScoring[5] > 9 )
              offscreen.drawString((String.valueOf(awayTeamScoring[5])),760,85);
           else
              offscreen.drawString((String.valueOf(awayTeamScoring[5])),767,85);

        // Draw CurrentDown

        if ( currentDown > 0 )
           offscreen.drawString((String.valueOf(currentDown)),237,114);

        if ( kickOff )
           offscreen.drawString("KO",232,114);

        if ( extraPoint )
           offscreen.drawString("XP",232,114);

        // Draw Distance

        if ( distance > 0 )
           if ( distance < 10 )
              offscreen.drawString((String.valueOf(distance)),356,114);
           else
              offscreen.drawString((String.valueOf(distance)),344,114);

        if ( kickOff )
           offscreen.drawString("KO",344,114);

        if ( extraPoint )
           offscreen.drawString("XP",344,114);

        // Draw BallOn

        if ( endOfPlay == true )
           drawBallOn = ballOn;

        if ( drawBallOn <= 50 )
           {
           offscreen.drawLine(439,110,442,107);
           offscreen.drawLine(442,107,445,110);
           }

        if ( drawBallOn >= 50 )
           {
           offscreen.drawLine(439,110,442,113);
           offscreen.drawLine(442,113,445,110);
           }

        if ( drawBallOn < 10 )
           offscreen.drawString((String.valueOf(drawBallOn)),454,114);

        if ( drawBallOn >= 10 && drawBallOn <= 50 )
           offscreen.drawString((String.valueOf(drawBallOn)),447,114);

        if ( drawBallOn > 50 && drawBallOn <= 90 )
           offscreen.drawString((String.valueOf((100 - drawBallOn))),447,114);

        if ( drawBallOn > 90 )
           offscreen.drawString((String.valueOf((100 - drawBallOn))),454,114);

        // Draw Quarter

        if ( currentQuarter < 5 )
           offscreen.drawString((String.valueOf(currentQuarter)),549,114);
        else
           offscreen.drawString("OT",542,114);

        // Draw Time Left

        if ( minutesLeft > 9 )
            offscreen.drawString((String.valueOf(minutesLeft)),655,115);
        else
            if ( minutesLeft != 0 )
               offscreen.drawString((String.valueOf(minutesLeft)),662,115);

        offscreen.drawString(":",669,115);

        if ( secondsLeft > 9 )
            offscreen.drawString((String.valueOf(secondsLeft)),676,115);
        else
            if ( secondsLeft == 0 )
              offscreen.drawString("00",676,115);
            else
               {
               offscreen.drawString("0",676,115);
               offscreen.drawString((String.valueOf(secondsLeft)),683,115);
               }

        // Draw SnapClock

        if ( snapClock > 9 )
           offscreen.drawString(":" + (String.valueOf(snapClock)),753,115);
        else
           offscreen.drawString(":0" + (String.valueOf(snapClock)),753,115);

        // Draw Contents of Play Description Box
        if ( playDescriptionBoxText[0] != null )
           offscreen.drawString(playDescriptionBoxText[0],587,195);
        if ( playDescriptionBoxText[1] != null )
           offscreen.drawString(playDescriptionBoxText[1],587,208);
        if ( playDescriptionBoxText[2] != null )
           offscreen.drawString(playDescriptionBoxText[2],587,221);
        if ( playDescriptionBoxText[3] != null )
           offscreen.drawString(playDescriptionBoxText[3],587,234);
        if ( playDescriptionBoxText[4] != null )
           offscreen.drawString(playDescriptionBoxText[4],587,247);
        if ( playDescriptionBoxText[5] != null )
           offscreen.drawString(playDescriptionBoxText[5],587,260);
        if ( playDescriptionBoxText[6] != null )
           offscreen.drawString(playDescriptionBoxText[6],587,273);

        // Draw Contents of Home Team Stats Box
        if ( homeTeamStatsBoxText[0] != null )
           offscreen.drawString(homeTeamStatsBoxText[0],587,315);
        if ( homeTeamStatsBoxText[1] != null )
           offscreen.drawString(homeTeamStatsBoxText[1],587,328);
        if ( homeTeamStatsBoxText[2] != null )
           offscreen.drawString(homeTeamStatsBoxText[2],587,341);
        if ( homeTeamStatsBoxText[3] != null )
           offscreen.drawString(homeTeamStatsBoxText[3],587,354);
        if ( homeTeamStatsBoxText[4] != null )
           offscreen.drawString(homeTeamStatsBoxText[4],587,367);
        if ( homeTeamStatsBoxText[5] != null )
           offscreen.drawString(homeTeamStatsBoxText[5],587,380);
        if ( homeTeamStatsBoxText[6] != null )
           offscreen.drawString(homeTeamStatsBoxText[6],587,393);

        // Draw Contents of Away Team Stats Box
        if ( awayTeamStatsBoxText[0] != null )
           offscreen.drawString(awayTeamStatsBoxText[0],587,435);
        if ( awayTeamStatsBoxText[1] != null )
           offscreen.drawString(awayTeamStatsBoxText[1],587,448);
        if ( awayTeamStatsBoxText[2] != null )
           offscreen.drawString(awayTeamStatsBoxText[2],587,461);
        if ( awayTeamStatsBoxText[3] != null )
           offscreen.drawString(awayTeamStatsBoxText[3],587,474);
        if ( awayTeamStatsBoxText[4] != null )
           offscreen.drawString(awayTeamStatsBoxText[4],587,487);
        if ( awayTeamStatsBoxText[5] != null )
           offscreen.drawString(awayTeamStatsBoxText[5],587,500);
        if ( awayTeamStatsBoxText[6] != null )
           offscreen.drawString(awayTeamStatsBoxText[6],587,513);

        // Draw pitch contents
        offscreen.setColor(Color.white);

        if ( ballOn <= 14 )
           // Draw top endzone , set pitch variables
           {
           downLines = 0;
           currentLine = 0;
           topLine = 1;
           offscreen.setColor(endZoneColour);
           offscreen.fillRect(7,165,555,100);
           offscreen.setColor(endZoneNameColour);
           offscreen.setFont(endZoneFont);
           offscreen.drawString(endZoneName,endZoneNameXPos,250);
           offscreen.setFont(font);
           offscreen.setColor(Color.white);
           offscreen.fillOval(240,173,5,5);
           offscreen.fillOval(310,173,5,5);
           offscreen.fillRect(242,173,70,5);
           offscreen.fillRect(277,166,5,7);
           offscreen.setColor(Color.black);
           offscreen.drawOval(240,173,5,5);
           offscreen.drawOval(310,173,5,5);
           offscreen.drawRect(245,173,65,5);
           offscreen.drawRect(277,166,5,7);
           offscreen.setColor(Color.white);
           yPos    = 265;
           yMinPos = 165;
           yMaxPos = 550;
           }
        else
           if ( ballOn >= 86 )
              // Draw bottom endzone , set pitch variables
              {
              downLines = 1;
              currentLine = 86;
              topLine = 91;
              offscreen.setColor(endZoneColour);
              offscreen.fillRect(7,453,555,100);
              offscreen.setColor(endZoneNameColour);
              offscreen.setFont(endZoneFont);
              offscreen.drawString(endZoneName,endZoneNameXPos,536);
              offscreen.setFont(font);
              offscreen.setColor(Color.white);
              offscreen.fillOval(240,539,5,5);
              offscreen.fillOval(310,539,5,5);
              offscreen.fillRect(242,539,70,5);
              offscreen.fillRect(277,544,5,7);
              offscreen.setColor(Color.black);
              offscreen.drawOval(240,539,5,5);
              offscreen.drawOval(310,539,5,5);
              offscreen.drawRect(245,539,65,5);
              offscreen.drawRect(277,544,5,7);
              offscreen.setColor(Color.white);
              yPos    = 172;
              yMinPos = 172;
              yMaxPos = 470;
              }
           else
              // No endzones , set pitch variables
              {
              downLines = (ballOn - 8) % 5;
              currentLine = ballOn - 8;
              topLine = currentLine + 5;
              yPos    = 175;
              yMinPos = 175;
              yMaxPos = 550;
              }

        // Draw pitch markings
        for ( int gridLines = 1 ; yPos < yMaxPos ; gridLines++ )
            {
            if ( downLines == 5 || downLines == 0)
               // Draw complete line across pitch
               {
               offscreen.drawLine(7,yPos,562,yPos);
               if ( currentLine % 10 == 0 && currentLine != 0 && currentLine != 100)
                  // Draw marker values every 10 yards , but not at either goal line
                  {
                  if ( currentLine <= 50 )
                  // Use currentLine variable for marker value
                     {
                     offscreen.drawString(String.valueOf(currentLine),20,(yPos - 2));
                     offscreen.drawString(String.valueOf(currentLine),20,(yPos + 11));
                     offscreen.drawString(String.valueOf(currentLine),536,(yPos - 2));
                     offscreen.drawString(String.valueOf(currentLine),536,(yPos + 11));
                     }
                  else
                     // currentLine variable needs to be converted to a value less than 50
                     {
                     offscreen.drawString(String.valueOf((100 - currentLine)),20,(yPos - 2));
                     offscreen.drawString(String.valueOf((100 - currentLine)),20,(yPos + 11));
                     offscreen.drawString(String.valueOf((100 - currentLine)),536,(yPos - 2));
                     offscreen.drawString(String.valueOf((100 - currentLine)),536,(yPos + 11));
                     }
                  }
               downLines = 1;
               }
            else
               // Draw hash marks
               {
               offscreen.drawLine(10,yPos,15,yPos);
               offscreen.drawLine(240,yPos,245,yPos);
               offscreen.drawLine(310,yPos,315,yPos);
               offscreen.drawLine(554,yPos,559,yPos);
               downLines++;
               }

            // Animate the ball moving down the pitch
            if ( currentLine == ballOn )
               // Only draw the ball it is on the current line being drawn
               {
               offscreen.setColor(Color.black);
               if ( pitchHomeTeamPossession == true )
                  {
                    System.out.println("HomeTeamPossession");
                    offscreen.drawString(String.valueOf(88),275,yPos);
 //                 offscreen.fillOval(275,yPos,7,10);
                  }
               else
                  {
                  int gridSpotXPos = 266;
                  offscreen.fillOval(270,yPos - 10,7,10);
                  }
                  offscreen.setColor(Color.white);
               }
            yPos += 20;
            currentLine ++;
            }

            // Draw players onto pitch
            for ( int playerIndex = 0 ; playerIndex < 30 ; playerIndex++ )
                {
                if ( offensivePlayer[playerIndex] != 0 )
                   // If player found within array
                   if ( offensivePlayerYPos[playerIndex] >= topLine && offensivePlayerYPos[playerIndex] <= ( topLine + 17 ) )
                      // Players co-ordinates fit within the boundary of the visible screen
                      {
                      offscreen.setColor(Color.white);
                      offscreen.fillOval(5 + ( 20 * ( offensivePlayerXPos[playerIndex] - 1 ) ), yMinPos + ( ( offensivePlayerYPos[playerIndex] - topLine ) * 20 ) + 1,18,18);
                      offscreen.setColor(Color.black);
                      offscreen.drawOval(5 + ( 20 * ( offensivePlayerXPos[playerIndex] - 1 ) ), yMinPos + ( ( offensivePlayerYPos[playerIndex] - topLine ) * 20 ) + 1,18,18);
                      if ( offensivePlayer[playerIndex] < 10 )
                         offscreen.drawString(String.valueOf(offensivePlayer[playerIndex]),11 + ( 20 * ( offensivePlayerXPos[playerIndex] - 1 ) ) , yMinPos + ( ( offensivePlayerYPos[playerIndex] - topLine ) * 20 ) + 15 );
                      else
                         offscreen.drawString(String.valueOf(offensivePlayer[playerIndex]),7 + ( 20 * ( offensivePlayerXPos[playerIndex] - 1 ) ) , yMinPos + ( ( offensivePlayerYPos[playerIndex] - topLine ) * 20 ) + 15 );
                      offscreen.setColor(Color.white);
                      }
                   }

        screen.drawImage(offscreenImage,0,0,this);
        //screen.drawString(stringBuffer.toString(),5,40);

    }

    public void update(Graphics screen) {

        paint(screen);

    }

    public void stop() {

        thread = null;

    }

}

class GameViewerWindow extends Frame {

    private GameViewer gameViewer;

    public GameViewerWindow(String windowName) {

        super(windowName);
        addWindowListener(new GameViewerAdapter());
        gameViewer = new GameViewer();
        gameViewer.init();
        gameViewer.start();
        add(gameViewer);
    }

    class GameViewerAdapter extends WindowAdapter {

       public void windowClosing(WindowEvent e) {
            gameViewer.stop();
            gameViewer.destroy();
            System.exit(0);
        }
    }
}
