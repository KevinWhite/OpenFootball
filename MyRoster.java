import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class MyRoster extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="MyRoster";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
            Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
      PrintWriter webPageOutput=null;
      try
        {
        webPageOutput=response.getWriter();
        }
      catch(IOException error)
        {
        Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
        }
      HttpSession session=request.getSession();
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      if(Routines.loginCheck(false,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("My Roster",//title
                             true,//showMenu
                             9,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/MyRoster.jpg\"" +
                            " WIDTH='386' HEIGHT='79' ALT='My Roster'>");
      webPageOutput.println(Routines.spaceLines(1));
      String submitAction = request.getParameter("submitAction");
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      int setNumber=Routines.safeParseInt(request.getParameter("set"));
      String currentPositionDescription="";
      int positionNumbers[]=null;
      String anchorDescriptions[]=null;
      String positionSubDescriptions[]=null;
      int numOfPositions=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT COUNT(PositionNumber) " +
                                      "FROM positions " +
                                      "WHERE Type!=3 " +
                                      "AND RealPosition=1 " +
                                      "GROUP BY RealPosition");
        if(queryResults.first())
          {
          numOfPositions=queryResults.getInt(1);
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to get positions(1) : " + error,false,context);
        }
      positionNumbers=new int[numOfPositions];
      anchorDescriptions=new String[numOfPositions];
      positionSubDescriptions=new String[numOfPositions];
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT PositionNumber,PositionCode,PositionName " +
                                      "FROM positions " +
                                      "WHERE Type!=3 " +
                                      "AND ChartOnly=0 " +
                                      "ORDER BY PositionNumber ASC");
        int currentPosition=0;
        while(queryResults.next())
          {
          positionNumbers[currentPosition]=queryResults.getInt(1);
          anchorDescriptions[currentPosition]=queryResults.getString(2);
          positionSubDescriptions[currentPosition]=queryResults.getString(3);
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to get positions(2) : " + error,false,context);
        }
      String positionDescriptions[]={"QuarterBacks",
                                     "RunningBacks",
                                     "Tight Ends",
                                     "Wide Receivers",
                                     "Offensive Linemen",
                                     "Offensive Linemen",
                                     "Offensive Linemen",
                                     "Offensive Linemen",
                                     "Offensive Linemen",
                                     "Offensive Linemen",
                                     "Defensive Linemen",
                                     "Defensive Linemen",
                                     "Defensive Linemen",
                                     "LineBackers",
                                     "LineBackers",
                                     "LineBackers",
                                     "Defensive Backs",
                                     "Defensive Backs",
                                     "Defensive Backs",
                                     "Kickers"};
      int bestInjuredPunter=0;
      int bestInjuredPunterInjury=0;
      int bestInjuredPunterSkills=0;
      String bestInjuredPunterName="";
      int bestHealthyPunter=0;
      int bestHealthyPunterSkills=0;
      String bestHealthyPunterName="";
      boolean doctorTrainer=false;
      int[] drTrPositionNumber={10,11};
      String[] drTrText={"Doctor","Trainer"};
      int numOfPlaces[]={0,0};
      String[] surNames={"",""};
      String[] forNames={"",""};
      boolean multiDRTR[]={false,false};
      boolean updated=false;
      for(int currentPosition=0;currentPosition<drTrPositionNumber.length;currentPosition++)
         {
         try
           {
           Statement sql=database.createStatement();
           ResultSet queryResult;
           queryResult=sql.executeQuery("SELECT Skill1,Injury,Surname,Forname " +
                                        "FROM players " +
                                        "WHERE TeamNumber=" + teamNumber + " " +
                                        "AND PositionNumber=" + drTrPositionNumber[currentPosition]);
           int tempNumOfPlaces=0;
           int numOfDRTR=0;
           while(queryResult.next())
              {
              numOfDRTR++;
              int skill1=queryResult.getInt(1)/10;
              int injury=queryResult.getInt(2);
              surNames[currentPosition]=queryResult.getString(3);
              forNames[currentPosition]=queryResult.getString(4);
              if(injury==0)
                {
                tempNumOfPlaces=+skill1;
                }
              }
           if(tempNumOfPlaces>0)
             {
             numOfPlaces[currentPosition]=tempNumOfPlaces;
             if(numOfDRTR>1)
               {
               multiDRTR[currentPosition]=true;
               }
             }
           }
         catch(SQLException error)
           {
           Routines.writeToLog(servletName,"Unable to access " + drTrText[currentPosition] + " details : " + error,false,context);
           }
         }
      try
         {
         Statement sql=database.createStatement();
         ResultSet queryResult;
         queryResult=sql.executeQuery("SELECT Sum(Doctors),Sum(Trainers) " +
                                      "FROM players " +
                                      "WHERE TeamNumber=" + teamNumber);
         if(queryResult.first())
           {
           int doctorVisits=queryResult.getInt(1);
           int trainerVisits=queryResult.getInt(2);
           numOfPlaces[0]-=doctorVisits;
           numOfPlaces[1]-=trainerVisits;
           }
         }
      catch(SQLException error)
         {
         Routines.writeToLog(servletName,"Unable to retrieve player injury/doctor usage : " + error,false,context);
         }
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         if(setNumber==0)
           {
           queryResponse=sql.executeQuery("SELECT SetNumber " +
                                          "FROM defaultsets " +
                                          "WHERE SetName='Normal'");
           if(queryResponse.first())
             {
             setNumber=queryResponse.getInt(1);
             }
           else
             {
             Routines.writeToLog(servletName,"No normal set found (TeamNumber=" +
                                            teamNumber +
                                            ")",false,context);
             }
           }
         }
      catch(SQLException error)
         {
         Routines.writeToLog(servletName,"Unable to retrieve sets : " + error,false,context);
         }
      if ("Send to Doctors".equals(submitAction)||
          "Send to Trainers".equals(submitAction)||
          "Move Up".equals(submitAction)||
          "Move Down".equals(submitAction)||
          "Waive".equals(submitAction))
          {
          int updateValues[]={0,0,0};
          updateValues=updateEntry(submitAction,
                                   numOfPlaces[0],
                                   numOfPlaces[1],
                                   teamNumber,
                                   setNumber,
                                   request,
                                   database);
          if(updateValues[2]==1)
            {
            updated=true;
            numOfPlaces[0]=updateValues[0];
            numOfPlaces[1]=updateValues[1];
            }
          }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/MyRoster\" METHOD=\"POST\">");
      webPageOutput.println("<CENTER>");
      if(numOfPlaces[0]!=0)
        {
        webPageOutput.println("<IMG SRC=\"../Images/MadDoc.gif\"" +
                              " WIDTH='80' HEIGHT='110' ALT='Doctor'>");
        }
      webPageOutput.println("<IMG SRC=\"../Images/CoachRoster.gif\"" +
                            " WIDTH='60' HEIGHT='100' ALT='Coach'>");
      if(numOfPlaces[0]!=0)
        {
        webPageOutput.println("<IMG SRC=\"../Images/ScoldingCoach.gif\"" +
                               " WIDTH='95' HEIGHT='90' ALT='Trainer'>");
        }
      webPageOutput.println("</CENTER>");
      Routines.myTableStart(false,webPageOutput);
      Routines.myTableHeader((String)session.getAttribute("teamName"),1,webPageOutput);
      Routines.tableEnd(webPageOutput);
      if((String)session.getAttribute("message")!=null)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Messages",0,webPageOutput);
        Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
        Routines.messageCheck(false,request,webPageOutput);
        Routines.tableDataEnd(true,false,true,webPageOutput);
        doctorTrainer=true;
        }
      for(int currentPosition=0;currentPosition<drTrPositionNumber.length;currentPosition++)
         {
         if(numOfPlaces[currentPosition]>0)
           {
           if(!doctorTrainer)
             {
             Routines.tableStart(false,webPageOutput);
             Routines.tableHeader("Messages",0,webPageOutput);
             doctorTrainer=true;
             }
           Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
           if(multiDRTR[currentPosition]==true)
             {
             webPageOutput.println("Your " + drTrText[currentPosition] + "s can work on " + numOfPlaces[currentPosition] + " more players this week.");
             }
           else
             {
             webPageOutput.println(drTrText[currentPosition] + " " + forNames[currentPosition] + " " + surNames[currentPosition] + " can work on " + numOfPlaces[currentPosition] + " more players this week.");
             }
           Routines.tableDataEnd(true,false,true,webPageOutput);
           }
         }
      if(doctorTrainer)
        {
        Routines.tableEnd(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Sets",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SetNumber,SetName " +
                                     "FROM defaultsets " +
                                     "ORDER BY SetNumber ASC");
        webPageOutput.println("<SELECT NAME=\"set\">");
        int tempSetNumber=0;
        String tempSetName="";
        String selected="";
        while(queryResult.next())
           {
           tempSetNumber=queryResult.getInt(1);
           tempSetName=queryResult.getString(2);
           if(tempSetNumber==setNumber)
              {
              selected="SELECTED ";
              }
           else
              {
              selected="";
              }
           webPageOutput.println(" " +
                                 "<OPTION " +
                                 selected +
                                 "VALUE=\"" +
                                 tempSetNumber +
                                 "\">" +
                                 "Your '" +
                                 tempSetName +
                                 "' set of players");
           }
         }
       catch(SQLException error)
         {
         Routines.writeToLog(servletName,"Unable to retrieve sets : " + error,false,context);
         }
       webPageOutput.println("</SELECT>");
       webPageOutput.println("<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"Change\">");
       Routines.tableDataEnd(true,false,true,webPageOutput);
       Routines.tableHeader("Actions",0,webPageOutput);
       Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
       if(numOfPlaces[0]!=0)
         {
         webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Send to Doctors\" NAME=\"submitAction\">");
         }
      if(numOfPlaces[0]!=0)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Send to Trainers\" NAME=\"submitAction\">");
        }
//      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Waive\" NAME=\"submitAction\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Up\" NAME=\"submitAction\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Down\" NAME=\"submitAction\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableHeader("Quick Links",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#QB\">QuarterBacks</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#RB\">RunningBacks</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#TE\">Tight Ends</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#WR\">Wide Receivers</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#OL\">Offensive Linemen</A>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#DL\">Defensive Linemen</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#LB\">Linebackers</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#DB\">Defensive Backs</A>");
      webPageOutput.println("<B>·</B>");
      webPageOutput.println("<A CLASS=\"opt\" HREF=\"#K\">Kickers</A>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      webPageOutput.println("</CENTER>");
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<DIV CLASS=\"SLTables2\"><TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
      webPageOutput.println("<TBODY>");

      int selectedPlayers[]=new int[100];
      int numOfSelectedPlayers=0;
      if(!updated)
        {
        for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
           {
           int playerID=0;
           playerID=Routines.safeParseInt(request.getParameter("selectPlayer" + currentPlayer));
           if(playerID!=0)
             {
             selectedPlayers[numOfSelectedPlayers]=playerID;
             numOfSelectedPlayers++;
             }
           }
        }
      int currentPlayer=0;
      for(int currentPosition=0;currentPosition<positionNumbers.length;currentPosition++)
         {
         try
            {
            Statement sql = database.createStatement();
            ResultSet queryResponse;
            queryResponse = sql.executeQuery("SELECT players.PlayerNumber,PositionDisplayCode,Surname,Forname,Experience,Skill1,Skill2,Skill3,Skill4,Skill5,Skill6,Skill7,Injury,Form " +
                                             "FROM players,depthcharts,positions " +
                                             "WHERE players.TeamNumber = " +
                                             teamNumber + " " +
                                             "AND players.PlayerNumber=depthcharts.PlayerNumber " +
                                             "AND players.TeamNumber=depthcharts.teamNumber " +
                                             "AND SetNumber=" + setNumber + " " +
                                             "AND depthcharts.PositionNumber = positions.PositionNumber " +
                                             "AND depthcharts.PositionNumber = " + positionNumbers[currentPosition] +
                                             " " +
                                             "ORDER BY depth ASC");
            if(!currentPositionDescription.equals(positionDescriptions[currentPosition]))
              {
              webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
              if("Kickers".equals(positionDescriptions[currentPosition]))
                {
                webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"11\">");
                }
              else
                {
                webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"12\">");
                }
              webPageOutput.println("<A NAME=\"" + anchorDescriptions[currentPosition] + "\"></A>");
              webPageOutput.println("<FONT CLASS=\"home\">");
              webPageOutput.println(positionDescriptions[currentPosition]);
              webPageOutput.println("</FONT>");
              webPageOutput.println("</TD>");
              webPageOutput.println("</TR>");
              currentPositionDescription=positionDescriptions[currentPosition];
              Routines.skillsTitleLine(positionDescriptions[currentPosition],currentPosition,false,webPageOutput);
              }
            if(!positionDescriptions[currentPosition].equals(positionSubDescriptions[currentPosition]))
              {
              webPageOutput.println("<TR CLASS=\"bg1\">");
              webPageOutput.println("<TD ALIGN=\"LEFT\" COLSPAN=\"12\"><FONT CLASS=\"opt2\"><B>" + positionSubDescriptions[currentPosition] + "</B></FONT></TD>");
              webPageOutput.println("</TR>");
              }
            while(queryResponse.next())
               {
               int playerID=queryResponse.getInt(1);
               String positionCode=queryResponse.getString(2);
               String surname=queryResponse.getString(3);
               String forname=queryResponse.getString(4);
               int experience=queryResponse.getInt(5);
               int skill1=queryResponse.getInt(6);
               int skill2=queryResponse.getInt(7);
               int skill3=queryResponse.getInt(8);
               int skill4=queryResponse.getInt(9);
               int skill5=queryResponse.getInt(10);
               int skill6=queryResponse.getInt(11);
               int skill7=queryResponse.getInt(12);
               int injury=queryResponse.getInt(13);
               int form=queryResponse.getInt(14);
               String colourOpen = new String("");
               String colourClose = new String("");
               colourOpen = Routines.colourOpen(injury);
               colourClose = Routines.colourClose(injury);
               boolean selected=false;
               for(int currentSelect=0;currentSelect<numOfSelectedPlayers;currentSelect++)
                  {
                  if(selectedPlayers[currentSelect]==playerID)
                    {
                    selected=true;
                    }
                  }
               webPageOutput.println("<TR CLASS=\"bg2\">");
               webPageOutput.println("<TD><FONT CLASS=\"opt2\">");
               webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"selectPlayer" + currentPlayer + "\" VALUE=\"" + playerID + "\"");
               if(selected)
                 {
                 webPageOutput.print(" CHECKED");
                 }
               webPageOutput.println(">");
               webPageOutput.println("</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">" + colourOpen);
               String fullName = forname + " " + surname;
               Routines.WriteHTMLLink(request,
                                      response,
                                      webPageOutput,
                                      "wfafl",
                                      "action=viewMyRosterEntry&playerNumber=" +
                                      playerID +
                                      "&positionName=" +
                                      positionDescriptions[currentPosition] +
                                      "&positionNumber=" +
                                      currentPosition,
                                      fullName,
                                      null,
                                      true);
               if (positionDescriptions[currentPosition].equals("Offensive Linemen") ||
                   positionDescriptions[currentPosition].equals("Defensive Linemen"))
                   {
                   webPageOutput.println("(" + positionCode + ")");
                   }
               webPageOutput.println(colourClose + "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       experience +
                                       colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                     colourOpen +
                                     Routines.skillsDescription(skill1) +
                                     colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       Routines.skillsDescription(skill2) +
                                       colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       Routines.skillsDescription(skill3) +
                                       colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       Routines.skillsDescription(skill4) +
                                       colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       Routines.skillsDescription(skill5) +
                                       colourClose +
                                     "</FONT></TD>");
               if(positionDescriptions[currentPosition].equals("Kickers"))
                  {
                  int punterSkills=(skill6*10)+skill7;
                  if(injury==0)
                    {
                    if(punterSkills>=bestHealthyPunterSkills)
                      {
                      bestHealthyPunter=playerID;
                      bestHealthyPunterSkills=punterSkills;
                      bestHealthyPunterName=forname + " " + surname;
                      }
                    }
                  else
                    {
                    if(bestInjuredPunterInjury==0||
                       injury<bestInjuredPunterInjury)
                      {
                      bestInjuredPunter=playerID;
                      bestInjuredPunterSkills=punterSkills;
                      bestInjuredPunterName=forname + " " + surname;
                      }
                    }
                  webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                        colourOpen +
                                        skill6 +
                                        skill7 +
                                        colourClose +
                                        "</FONT></TD>");
                  }
               else
                  {
                  webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                          colourOpen +
                                          Routines.skillsDescription(skill6) +
                                          colourClose +
                                        "</FONT></TD>");
                  webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                          colourOpen +
                                          Routines.skillsDescription(skill7) +
                                          colourClose +
                                        "</FONT></TD>");
                  }
               if(injury!=0)
                 {
                 webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       injury +
                                       colourClose +
                                       "</FONT></TD>");
                 }
               else
                 {
                 webPageOutput.println("<TD></TD>");
                 }
               String formString="";
               if(form<5)
                 {
                 formString="-";
                 }
               if(form>5)
                 {
                 formString="+";
                 }
               webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                       colourOpen +
                                       formString +
                                       colourClose +
                                     "</FONT></TD>");
               webPageOutput.println("</TR>");
               currentPlayer++;
               }
            if(!queryResponse.first())
              {
              webPageOutput.println("<TR CLASS=\"bg2\">");
              if("Kickers".equals(positionDescriptions[currentPosition]))
                {
                webPageOutput.println("<TD ALIGN=\"LEFT\" COLSPAN=\"11\"><FONT CLASS=\"opt2\">");
                }
              else
                {
                webPageOutput.println("<TD ALIGN=\"LEFT\" COLSPAN=\"12\"><FONT CLASS=\"opt2\">");
                }
              webPageOutput.println("None");
              webPageOutput.println("</FONT></TD></TR>");
              }
            }
         catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Unable to access player details : " + error,false,context);
            }
         }
      webPageOutput.println("<TR CLASS=\"bg1\">");
      webPageOutput.println("<TD ALIGN=\"LEFT\" COLSPAN=\"11\"><FONT CLASS=\"opt2\">");
      String bestPunter="";
      if(bestHealthyPunter==0)
        {
        bestPunter=bestInjuredPunterName;
        }
      else
        {
        bestPunter=bestHealthyPunterName;
        }
      webPageOutput.println("Your punter is automatically set and will be " + bestPunter);
      webPageOutput.println("</FONT></TD></TR>");

      webPageOutput.println("</TBODY></TABLE></DIV>");
      try
        {
        webPageOutput.println("<DIV CLASS=\"SLTables2\"><TABLE WIDTH=\"70%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
        webPageOutput.println("<TBODY>");
        Statement sql=database.createStatement();
        ResultSet queryResponse;
        queryResponse = sql.executeQuery("SELECT PlayerNumber,positions.PositionDisplayCode,Surname,Forname,Experience,Skill1,Injury " +
                                         "FROM players,positions " +
                                         "WHERE TeamNumber = " +
                                         teamNumber +
                                         " AND Staff=1 " +
                                         "AND players.PositionNumber = positions.PositionNumber " +
                                         "ORDER BY PlayerNumber ASC");
        webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
        webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"7\">");
        webPageOutput.println("<FONT CLASS=\"home\">");
        webPageOutput.println("Staff");
        webPageOutput.println("</FONT>");
        webPageOutput.println("</TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("<TR CLASS=\"bg1\">");
        webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">Sel</FONT></TD>");
        webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">Name</FONT></TD>");
        webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Experience</FONT></TD>");
        webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Rating</FONT></TD>");
        webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Injury</FONT></TD>");
        while(queryResponse.next())
           {
           int playerID=queryResponse.getInt(1);
           String positionCode=queryResponse.getString(2);
           String surname=queryResponse.getString(3);
           String forname=queryResponse.getString(4);
           int experience=queryResponse.getInt(5);
           int skill1=queryResponse.getInt(6)/10;
           int injury=queryResponse.getInt(7);
           String colourOpen = new String("");
           String colourClose = new String("");
           colourOpen = Routines.colourOpen(injury);
           colourClose = Routines.colourClose(injury);
           webPageOutput.println("<TR CLASS=\"bg2\">");
           webPageOutput.println("<TD><FONT CLASS=\"opt2\">");
           webPageOutput.println("<INPUT TYPE=\"CHECKBOX\" NAME=\"selectPlayer" + currentPlayer + "\" VALUE=\"" + playerID + "\">");
           webPageOutput.println("</FONT></TD>");
           webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">" + colourOpen);
           String fullName = forname + " " + surname;
           Routines.WriteHTMLLink(request,
                                  response,
                                  webPageOutput,
                                  "wfafl",
                                  "action=viewMyRosterEntry&playerNumber=" +
                                  playerID,
                                  fullName,
                                  null,
                                  true);
           webPageOutput.println("(" + positionCode + ")");
           webPageOutput.println(colourClose + "</FONT></TD>");
           webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                 colourOpen +
                                 experience +
                                 colourClose +
                                 "</FONT></TD>");
           webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                 colourOpen +
                                 Routines.skillsDescription(skill1) +
                                 colourClose +
                                 "</FONT></TD>");
           if(injury!=0)
             {
             webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                                   colourOpen +
                                   injury +
                                   colourClose +
                                   "</FONT></TD>");
             }
           else
             {
             webPageOutput.println("<TD></TD>");
             }
           webPageOutput.println("</TR>");
           currentPlayer++;
           }
         if(!queryResponse.first())
           {
           webPageOutput.println("<TR CLASS=\"bg2\">");
           webPageOutput.println("<TD ALIGN=\"LEFT\" COLSPAN=\"7\"><FONT CLASS=\"opt2\">");
           webPageOutput.println("None");
           webPageOutput.println("</FONT></TD></TR>");
           }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to access staff details : " + error,false,context);
        }
      webPageOutput.println("</TBODY></TABLE></DIV>");
      webPageOutput.println("<CENTER>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(numOfPlaces[0]!=0)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Send to Doctors\" NAME=\"submitAction\">");
        }
      if(numOfPlaces[1]!=0)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Send to Trainers\" NAME=\"submitAction\">");
        }
//      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Waive\" NAME=\"submitAction\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Up\" NAME=\"submitAction\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Down\" NAME=\"submitAction\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("</CENTER>");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"action\" VALUE=\"" + "viewMyRoster" + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + league + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + teamNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private synchronized int[] updateEntry(String submitAction,
                                          int doctors,
                                          int trainers,
                                          int teamNumber,
                                          int setNumber,
                                          HttpServletRequest request,
                                          Connection database)
      {
      int returnValues[]={doctors,trainers,0};
      HttpSession session=request.getSession();
      int selectedPlayers[]=new int[100];
      int currentSelectedPlayer=0;
      int numOfSelectedPlayers=0;
      int maxDepth=0;
      String message="";
      int positionNumbers[]={19,20,17,18,
                             14,13,12,15,16,29,
                             23,24,30,
                             25,26,31,
                             27,28,32,
                             2};
      String positionDescriptions[]={"QuarterBack",
                                     "RunningBack",
                                     "Tight End",
                                     "Wide Receiver",
                                     "Offensive Lineman",
                                     "Offensive Lineman",
                                     "Offensive Lineman",
                                     "Offensive Lineman",
                                     "Offensive Lineman",
                                     "Offensive Lineman",
                                     "Defensive Lineman",
                                     "Defensive Lineman",
                                     "Defensive Lineman",
                                     "LineBacker",
                                     "LineBacker",
                                     "LineBacker",
                                     "Defensive Back",
                                     "Defensive Back",
                                     "Defensive Back",
                                     "Kickers"};
      boolean allowUp[]={false,false,false,false,
                         false,true,true,true,true,true,
                         false,true,true,
                         false,true,true,
                         false,true,true,
                         false};
      boolean allowDown[]={false,false,false,false,
                           true,true,true,true,true,false,
                           true,true,false,
                           true,true,false,
                           true,true,false,
                           false};
      boolean upDown[]={false,false,false,false,
                        false,false,false,false,false,false,
                        false,false,false,
                        false,false,false,
                        false,false,false,
                        false};
      boolean moveUp=false;
      for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
        {
        int playerID=0;
        playerID=Routines.safeParseInt(request.getParameter("selectPlayer" + currentPlayer));
        if(playerID!=0)
          {
          selectedPlayers[currentSelectedPlayer]=playerID;
          currentSelectedPlayer++;
          numOfSelectedPlayers++;
          }
        }
      String actionText[]={"Send to Doctors","Send to Trainers"};
      int numOfPlaces[]={doctors,trainers};
      String messageText[]={"doctors","trainers"};
      String selectText[]={"Injury,Doctors","Form,Trainers"};
      String updateText[]={"Injury=(Injury-1),Doctors=1","Form=(Form+5),Trainers=1"};
      for(int currentAction=0;currentAction<actionText.length;currentAction++)
         {
         if(actionText[currentAction].equals(submitAction))
           {
           if(numOfSelectedPlayers>numOfPlaces[currentAction])
             {
             session.setAttribute("message","You can only send " + numOfPlaces[currentAction] + " players to the " + messageText[currentAction]);
             return returnValues;
             }
           for(int currentPlayer=0;currentPlayer<numOfSelectedPlayers;currentPlayer++)
             {
             if(selectedPlayers[currentPlayer]!=0)
               {
               try
                 {
                 Statement sql=database.createStatement();
                 ResultSet queryResult;
                 queryResult=sql.executeQuery("SELECT " + selectText[currentAction] + ",Forname,Surname,positions.PositionDisplayCode " +
                                              "FROM players,positions " +
                                              "WHERE PlayerNumber=" + selectedPlayers[currentPlayer] + " " +
                                              "AND players.PositionNumber=positions.PositionNumber");
                 if(queryResult.first())
                   {
                   int skillToModify=queryResult.getInt(1);
                   int beenModified=queryResult.getInt(2);
                   String forName=queryResult.getString(3);
                   String surName=queryResult.getString(4);
                   String positionCode=queryResult.getString(5);
                   if(beenModified==1)
                     {
                     session.setAttribute("message",forName + " " + surName + "(" + positionCode + ") has been to the " + messageText[currentAction] + " already this week, only one session per week is allowed");
                     return returnValues;
                     }
                   if(currentAction==0&&skillToModify==0)
                     {
                     session.setAttribute("message",forName + " " + surName + "(" + positionCode + ") is not injured and cannot be sent to the Doctors");
                     return returnValues;
                     }
                   if(currentAction==1&&skillToModify==9)
                     {
                     session.setAttribute("message",forName + " " + surName + "(" + positionCode + ") is already on top form and cannot be sent to the Trainers");
                     return returnValues;
                     }
                   }
                 }
               catch(SQLException error)
                 {
                 Routines.writeToLog(servletName,"Unable to access player details : " + error,false,context);
                 }
               }
            }
          }
        }
      for(int currentAction=0;currentAction<actionText.length;currentAction++)
         {
         if(actionText[currentAction].equals(submitAction))
           {
           for(int currentPlayer=0;currentPlayer<numOfSelectedPlayers;currentPlayer++)
              {
              if(selectedPlayers[currentPlayer]!=0)
                {
                try
                  {
                  Statement sql=database.createStatement();
                  ResultSet queryResult;
                  queryResult=sql.executeQuery("UPDATE players " +
                                               "SET " + updateText[currentAction] + " " +
                                               "WHERE PlayerNumber=" + selectedPlayers[currentPlayer]);
                  returnValues[currentAction]--;
                  }
                catch(SQLException error)
                  {
                  Routines.writeToLog(servletName,"Unable to update player details : " + error,false,context);
                  }
                }
             }
           }
        }
      if("Waive".equals(submitAction))
        {
        for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
          {
          if(selectedPlayers[currentPlayer]!=0)
            {
            Routines.writeToLog(servletName,"Waiving " + selectedPlayers[currentPlayer],false,context);
            }
          }
        }
      boolean movePlayers=false;
      if("Move Up".equals(submitAction))
        {
        upDown=allowUp;
        movePlayers=true;
        moveUp=true;
        }
      if("Move Down".equals(submitAction))
        {
        upDown=allowDown;
        movePlayers=true;
        moveUp=false;
        }

      if(movePlayers)
        {
        for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
          {
          if(selectedPlayers[currentPlayer]!=0)
            {
            try
              {
              Statement sql1=database.createStatement();
              Statement sql2=database.createStatement();
              ResultSet queryResult1;
              ResultSet queryResult2;
              queryResult1=sql1.executeQuery("SELECT Forname,Surname,positions.PositionDisplayCode,depth,depthcharts.PositionNumber " +
                                             "FROM players,depthcharts,positions " +
                                             "WHERE players.PlayerNumber=" + selectedPlayers[currentPlayer] + " " +
                                             "AND players.PlayerNumber=depthcharts.PlayerNumber " +
                                             "AND players.TeamNumber=" + teamNumber + " " +
                                             "AND SetNumber=" + setNumber + " " +
                                             "AND players.PositionNumber=positions.PositionNumber");
              if(queryResult1.first())
                {
                int positionIndex=0;
                String forName=queryResult1.getString(1);
                String surName=queryResult1.getString(2);
                String positionCode=queryResult1.getString(3);
                int depth=queryResult1.getInt(4);
                int positionNumber=queryResult1.getInt(5);
                queryResult2=sql2.executeQuery("SELECT depth " +
                                               "FROM   depthcharts " +
                                               "WHERE  TeamNumber="  + teamNumber + " " +
                                               "AND    SetNumber=" + setNumber + " " +
                                               "AND    PositionNumber=" + positionNumber + " " +
                                               "ORDER BY depth DESC");
                maxDepth=0;
                if(queryResult2.first())
                  {
                  maxDepth=queryResult2.getInt(1);
                  }
                for(int currentPosition=0;currentPosition<positionNumbers.length;currentPosition++)
                   {
                   if(positionNumber==positionNumbers[currentPosition])
                     {
                     positionIndex=currentPosition;
                     }
                   }
                if(moveUp&&depth==1)
                  {
                  if(positionIndex==0)
                    {
                    message=forName +
                            " " +
                            surName +
                            "(" +
                            positionCode +
                            ") is already at the top of your roster";
                    session.setAttribute("message",
                                         message);
                    return returnValues;
                    }
                  else
                    {
                    if(positionNumbers[positionIndex]==32||
                       positionNumbers[positionIndex]==28||
                       positionNumbers[positionIndex]==31||
                       positionNumbers[positionIndex]==26||
                       positionNumbers[positionIndex]==30||
                       positionNumbers[positionIndex]==24||
                       positionNumbers[positionIndex]==29||
                       positionNumbers[positionIndex]==16||
                       positionNumbers[positionIndex]==15||
                       positionNumbers[positionIndex]==12||
                       positionNumbers[positionIndex]==13)
                       {
                       }
                    else
                       {
                       message=forName +
                               " " +
                               surName +
                               "(" +
                               positionCode +
                               ") cannot play at " +
                               positionDescriptions[positionIndex-1];
                       session.setAttribute("message",
                                            message);
                       return returnValues;
                       }
                     }
                  }
                if(!moveUp&&depth==maxDepth)
                  {
                  if(positionIndex==(positionNumbers.length-1))
                    {
                    message=forName +
                            " " +
                            surName +
                            "(" +
                            positionCode +
                            ") is already at the bottom of your roster";
                    session.setAttribute("message",
                                         message);
                    return returnValues;
                    }
                  else
                    {
                    if(positionNumbers[positionIndex]==14||
                       positionNumbers[positionIndex]==13||
                       positionNumbers[positionIndex]==12||
                       positionNumbers[positionIndex]==15||
                       positionNumbers[positionIndex]==16||
                       positionNumbers[positionIndex]==23||
                       positionNumbers[positionIndex]==24||
                       positionNumbers[positionIndex]==25||
                       positionNumbers[positionIndex]==26||
                       positionNumbers[positionIndex]==27||
                       positionNumbers[positionIndex]==28)
                       {
                       }
                    else
                       {
                       message=forName +
                               " " +
                               surName +
                               "(" +
                               positionCode +
                               ") cannot play at " +
                               positionDescriptions[positionIndex+1];
                       session.setAttribute("message",
                                            message);
                       return returnValues;
                       }
                    }
                  }
                }
              else
                {
                Routines.writeToLog(servletName,"Unable to find player details (PlayerNumber=" +
                                               selectedPlayers[currentPlayer] +
                                               ")",false,context);
                }
              }
            catch(SQLException error)
              {
              Routines.writeToLog(servletName,"Database error getting player details (PlayerNumber=" +
                                             selectedPlayers[currentPlayer] +
                                             ") " +
                                             error,false,context);
              }
            }
          }
        int positionNumber=0;
        int currentPlayer=0;
        int depth=0;
        int depthChartNumber=0;
        int positionIndex=0;
        int updateResult=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          if(moveUp)
            {
            for(currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
               {
               if(selectedPlayers[currentPlayer]!=0)
                {
                depth=0;
                positionNumber=0;
                maxDepth=0;
                depthChartNumber=0;
                positionIndex=0;
                updateResult=0;
                queryResult=sql.executeQuery("SELECT Depth,PositionNumber,DepthChartNumber " +
                                             "FROM depthcharts " +
                                             "WHERE PlayerNumber=" + selectedPlayers[currentPlayer] + " " +
                                             "AND TeamNumber=" + teamNumber + " " +
                                             "AND SetNumber=" + setNumber);
                if(queryResult.first())
                  {
                  depth=queryResult.getInt(1);
                  positionNumber=queryResult.getInt(2);
                  depthChartNumber=queryResult.getInt(3);
                  }
                else
                  {
                  Routines.writeToLog(servletName,"Unable to find player details (PlayerNumber=" +
                                                 selectedPlayers[currentPlayer] +
                                                 ")",false,context);
                  }
                for(int currentPosition=0;currentPosition<positionNumbers.length;currentPosition++)
                   {
                   if(positionNumber==positionNumbers[currentPosition])
                     {
                     positionIndex=currentPosition;
                     }
                   }
                if(depth==1)
                  {
                  queryResult=sql.executeQuery("SELECT depth " +
                                               "FROM   depthcharts " +
                                               "WHERE  TeamNumber="  + teamNumber + " " +
                                               "AND    SetNumber=" + setNumber + " " +
                                               "AND    PositionNumber=" + positionNumbers[positionIndex-1] + " " +
                                               "ORDER BY depth DESC");
                  depth=0;
                  if(queryResult.first())
                    {
                    depth=queryResult.getInt(1);
                    }
                  updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                 "SET PositionNumber=" + positionNumbers[positionIndex-1] + " " +
                                                 ",Depth=" + (depth+1) + " " +
                                                 "WHERE DepthChartNumber=" + depthChartNumber);
                  if(updateResult==0)
                    {
                    Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                   selectedPlayers[currentPlayer] +
                                                   ")",false,context);
                    }
                  updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                 "SET Depth=(Depth-1) " +
                                                 "WHERE TeamNumber=" + teamNumber + " " +
                                                 "AND SetNumber=" + setNumber + " " +
                                                 "AND PositionNumber=" + positionNumbers[positionIndex]);
                  }
                else
                  {
                  updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                 "SET Depth=(Depth+1) " +
                                                 "WHERE TeamNumber=" + teamNumber + " " +
                                                 "AND SetNumber=" + setNumber + " " +
                                                 "AND PositionNumber=" + positionNumbers[positionIndex] + " " +
                                                 "AND Depth=" + (depth-1));
                  if(updateResult==0)
                    {
                    Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                   selectedPlayers[currentPlayer] +
                                                   ")",false,context);
                    }
                  updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                 "SET Depth=(Depth-1) " +
                                                 "WHERE DepthChartNumber=" + depthChartNumber);
                  if(updateResult==0)
                    {
                    Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                   selectedPlayers[currentPlayer] +
                                                   ")",false,context);
                    }
                  }
                }
              }
            }
          else
            {
            for(currentPlayer=selectedPlayers.length-1;currentPlayer>-1;currentPlayer--)
               {
               if(selectedPlayers[currentPlayer]!=0)
                 {
                 depth=0;
                 positionNumber=0;
                 maxDepth=0;
                 depthChartNumber=0;
                 positionIndex=0;
                 updateResult=0;
                 queryResult=sql.executeQuery("SELECT Depth,PositionNumber,DepthChartNumber " +
                                              "FROM depthcharts " +
                                              "WHERE PlayerNumber=" + selectedPlayers[currentPlayer] + " " +
                                              "AND TeamNumber=" + teamNumber + " " +
                                              "AND SetNumber=" + setNumber);
                 if(queryResult.first())
                   {
                   depth=queryResult.getInt(1);
                   positionNumber=queryResult.getInt(2);
                   depthChartNumber=queryResult.getInt(3);
                   }
                 else
                   {
                   Routines.writeToLog(servletName,"Unable to find player details (PlayerNumber=" +
                                                  selectedPlayers[currentPlayer] +
                                                  ")",false,context);
                   }
                 for(int currentPosition=0;currentPosition<positionNumbers.length;currentPosition++)
                   {
                   if(positionNumber==positionNumbers[currentPosition])
                     {
                     positionIndex=currentPosition;
                     }
                   }
                 queryResult=sql.executeQuery("SELECT depth " +
                                              "FROM   depthcharts " +
                                              "WHERE  TeamNumber="  + teamNumber + " " +
                                              "AND    SetNumber=" + setNumber + " " +
                                              "AND    PositionNumber=" + positionNumber + " " +
                                              "ORDER BY depth DESC");
                 if(queryResult.first())
                   {
                   maxDepth=queryResult.getInt(1);
                   }
                 else
                   {
                   Routines.writeToLog(servletName,"Unable to find player details (PlayerNumber=" +
                                                  selectedPlayers[currentPlayer] +
                                                  ")",false,context);
                   }
                 if(depth==maxDepth)
                   {
                   updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                  "SET Depth=(Depth+1) " +
                                                  "WHERE TeamNumber=" + teamNumber + " " +
                                                  "AND SetNumber=" + setNumber + " " +
                                                  "AND PositionNumber=" + positionNumbers[positionIndex+1]);
                   updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                  "SET PositionNumber=" + positionNumbers[positionIndex+1] + " " +
                                                  ",Depth=1 " +
                                                  "WHERE TeamNumber=" + teamNumber + " " +
                                                  "AND SetNumber=" + setNumber + " " +
                                                  "AND PlayerNumber=" + selectedPlayers[currentPlayer]);
                   if(updateResult==0)
                     {
                     Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                    selectedPlayers[currentPlayer] +
                                                    ")",false,context);
                     }
                   }
                 else
                   {
                   updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                  "SET Depth=(Depth-1) " +
                                                  "WHERE TeamNumber=" + teamNumber + " " +
                                                  "AND SetNumber=" + setNumber + " " +
                                                  "AND PositionNumber=" + positionNumbers[positionIndex] + " " +
                                                  "AND Depth=" + (depth+1));
                   if(updateResult==0)
                     {
                     Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                    selectedPlayers[currentPlayer] +
                                                    ")",false,context);
                     }
                   updateResult=sql.executeUpdate("UPDATE depthcharts " +
                                                  "SET Depth=(Depth+1) " +
                                                  "WHERE DepthChartNumber=" + depthChartNumber);
                   if(updateResult==0)
                     {
                     Routines.writeToLog(servletName,"Unable to update depthcharts (PlayerNumber=" +
                                                    selectedPlayers[currentPlayer] +
                                                    ")",false,context);
                     }
                    }
                 }
               }
             }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Database error updating depthcharts (PlayerNumber=" +
                                         selectedPlayers[currentPlayer] +
                                         ") " +
                                         error,false,context);
          }
        }
     returnValues[2]=1;
     return returnValues;
     }
   }