import java.io.*;
import java.sql.*;
import java.util.Date;
import javax.servlet.http.*;
import java.util.*;
import java.text.*;
import javax.servlet.*;
import java.net.*;

class Routines
   {
   static String dbLock="";
   static String logLock="";
   static void WriteHTMLHead(String              title,
                             boolean             showMenu,
                             int                 menuHighLight,
                             boolean             seasonsMenu,
                             boolean             weeksMenu,
                             boolean             scores,
                             boolean             standings,
                             boolean             gameCenter,
                             boolean             schedules,
                             boolean             preview,
                             boolean             teamCenter,
                             boolean             draft,
                             Connection          database,
                             HttpServletRequest  request,
                             HttpServletResponse response,
                             PrintWriter         webPageOutput,
                             ServletContext      context)
      {
	  String servletName="Routines.WriteHTLMHead";	
      HttpSession session=request.getSession();
      boolean menu=false;
      boolean leaguesFound=false;
      boolean allowCoachChanges=false;
      int selectedLeague=Routines.safeParseInt(request.getParameter("league"));
      int selectedSeason=Routines.safeParseInt(request.getParameter("season"));
      int selectedWeek=Routines.safeParseInt(request.getParameter("week"));
      int coachNumber=Routines.safeParseInt((String)session.getAttribute("coachNumber"));
      int selectedLatestSeason=0;
      int selectedLatestWeek=0;
      int selectedLeaguePreSeasonWeeks=0;
      int selectedLeagueRegularSeasonWeeks=0;
      int selectedLeaguePostSeasonWeeks=0;
      int selectedLeagueLevel=0;
	  int selectedLeagueType=0;
	  int selectedLeagueRosterMin=0;
	  int selectedLeagueRosterMax=0;
	  int selectedLeagueSalaryCap=0;
      int teamNumber=Routines.safeParseInt(request.getParameter("team"));
      if(Routines.safeParseInt(request.getParameter("view"))==1&&teamNumber==0)
        {
		teamNumber=Routines.safeParseInt((String)session.getAttribute("viewTeam"));
        }
      int fixtureNumber=Routines.safeParseInt(request.getParameter("fixture"));
      int majorVersion=Routines.safeParseInt(request.getParameter("majorVersion"));
      int minorVersion=Routines.safeParseInt(request.getParameter("minorVersion"));
      int homeTeamNumber=0;
      int awayTeamNumber=0;
      Date currentDate=new Date();
      String selectedLeagueName=new String("");
      String[] colourDetails=new String[3];
      String homeTeamName="";
      String homeTeamShirtColour="";
      String homeTeamNumberColour="";
      String awayTeamName="";
      String awayTeamShirtColour="";
      String awayTeamNumberColour="";
      String action=request.getParameter("action");
      String version=request.getParameter("version");
      if(action==null)
        {
        action="";
        }
      if(version==null)
        {
        version="";
        }
      session.setAttribute("majorVersion",String.valueOf(majorVersion));
      session.setAttribute("minorVersion",String.valueOf(minorVersion));
      session.setAttribute("version",version);
      session.setAttribute("action",action);
      action="";
      ResultSet queryResult=null;
      if(teamNumber!=0)
        {
        colourDetails=Routines.getTeamColours(teamNumber,database,context);
        homeTeamName=colourDetails[0];
        homeTeamShirtColour=colourDetails[1];
        homeTeamNumberColour=colourDetails[2];
        }
      if(fixtureNumber!=0)
        {
        String teamInfo[] = Routines.getFixtureTeams(fixtureNumber,database,context);
        homeTeamNumber       = Routines.safeParseInt(teamInfo[0]);
        awayTeamNumber       = Routines.safeParseInt(teamInfo[1]);
        homeTeamName         = teamInfo[2];
        homeTeamShirtColour  = teamInfo[3];
        homeTeamNumberColour = teamInfo[4];
        awayTeamName         = teamInfo[5];
        awayTeamShirtColour  = teamInfo[6];
        awayTeamNumberColour = teamInfo[7];
        }
      if("".equals(homeTeamShirtColour))
        {
        homeTeamShirtColour="000000";
        }
      if("".equals(homeTeamNumberColour))
        {
        homeTeamNumberColour="ffffff";
        }
      try
         {
         Statement sql=database.createStatement();
         queryResult=sql.executeQuery("SELECT Name,LeagueNumber,Season,Week,PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,LeagueLevel,LeagueType,Alpha,RosterMin,RosterMax,SalaryCap " +
                                      "FROM leagues " +
                                      "WHERE Status > 0 " +
                                      "ORDER BY LeagueNumber ASC");
         while(queryResult.next())
            {
            if(!leaguesFound)
              {
              leaguesFound=true;
              }
            String leagueName=new String(queryResult.getString(1));
            int leagueNumber=queryResult.getInt(2);
            int season=queryResult.getInt(3);
            int week=queryResult.getInt(4);
            int preSeasonWeeks=queryResult.getInt(5);
            int regularSeasonWeeks=queryResult.getInt(6);
            int postSeasonWeeks=queryResult.getInt(7);
            int leagueLevel=queryResult.getInt(8);
            int leagueType=queryResult.getInt(9);
            int alphaLeague=queryResult.getInt(10);
            int rosterMin=queryResult.getInt(11);
            int rosterMax=queryResult.getInt(12);
            int salaryCap=queryResult.getInt(13);
            if (selectedLeague==leagueNumber)
               {
               selectedLeagueName=leagueName;
               selectedLatestSeason=season;
               selectedLatestWeek=week;
               selectedLeaguePreSeasonWeeks=preSeasonWeeks;
               selectedLeagueRegularSeasonWeeks=regularSeasonWeeks;
               selectedLeaguePostSeasonWeeks=postSeasonWeeks;
               selectedLeagueLevel=leagueLevel;
               selectedLeagueType=leagueType;
               selectedLeagueRosterMin=rosterMin;
               selectedLeagueRosterMax=rosterMax;
               selectedLeagueSalaryCap=salaryCap;
               if(showMenu)
                 {
                 menu=true;
                 }
               if(selectedSeason==0)
                 {
                 selectedSeason=selectedLatestSeason;
                 }
               if(selectedWeek==0)
                 {
                 selectedWeek=selectedLatestWeek;
                 }
               if(selectedWeek<(((selectedLeaguePreSeasonWeeks+selectedLeagueRegularSeasonWeeks)/2)+1)&&
                  alphaLeague==0)
                 {
                 allowCoachChanges=true;
                 }
               session.setAttribute("league",String.valueOf(leagueNumber));
               session.setAttribute("preSeasonWeeks",String.valueOf(selectedLeaguePreSeasonWeeks));
               session.setAttribute("regularSeasonWeeks",String.valueOf(selectedLeagueRegularSeasonWeeks));
               session.setAttribute("postSeasonWeeks",String.valueOf(selectedLeaguePostSeasonWeeks));
               session.setAttribute("leagueName",leagueName);
               session.setAttribute("season",String.valueOf(season));
               session.setAttribute("week",String.valueOf(week));
               session.setAttribute("viewSeason",String.valueOf(selectedSeason));
               session.setAttribute("viewWeek",String.valueOf(selectedWeek));
               session.setAttribute("team",String.valueOf(teamNumber));
               session.setAttribute("teamName",homeTeamName);
               session.setAttribute("fixture",String.valueOf(fixtureNumber));
               session.setAttribute("homeTeamNumber",String.valueOf(homeTeamNumber));
               session.setAttribute("awayTeamNumber",String.valueOf(awayTeamNumber));
               session.setAttribute("homeTeamName",homeTeamName);
               session.setAttribute("awayTeamName",awayTeamName);
               session.setAttribute("leagueLevel",String.valueOf(leagueLevel));
			   session.setAttribute("leagueType",String.valueOf(selectedLeagueType));
			   session.setAttribute("rosterMin",String.valueOf(selectedLeagueRosterMin));
			   session.setAttribute("rosterMax",String.valueOf(selectedLeagueRosterMax));
			   session.setAttribute("salaryCap",String.valueOf(selectedLeagueSalaryCap));
               }
            }
         }
      catch(SQLException error)
         {
         webPageOutput.println("Database error retrieving leagues: " + error.getMessage());
         }
      if(session.isNew())
        {
        String userName=null;
        userName=cookieCheck(request);
        if(userName!=null)
          {
          boolean[] loginAttemptResults=new boolean[2];
          loginAttemptResults=SignIn.login(true,userName,null,null,selectedLeague,session,response,database,context);
          if(loginAttemptResults[0])
            {
            coachNumber=Routines.safeParseInt((String)session.getAttribute("coachNumber"));
            }
          }
        }
      webPageOutput.println("<!--");
      webPageOutput.println("************************************************");
      webPageOutput.println("*** Club Pit                                 ***");
      webPageOutput.println("*** Produced on " + currentDate + " ***");
      webPageOutput.println("************************************************");
      webPageOutput.println("-->");
      webPageOutput.println("<!DOCTYPE doctype PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
      webPageOutput.println("<HTML>");
      webPageOutput.println("<HEAD>");
      webPageOutput.print("<TITLE>ClubPit Open Football");
      if(selectedLeague!=0)
        {
        webPageOutput.println(" : " + (String)session.getAttribute("leagueName"));
        }
      if(title!=null)
        {
        webPageOutput.print(" : " + title);
        }
      webPageOutput.println("</TITLE>");
      webPageOutput.println("<LINK REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"../Images/Styles.css\">");
      webPageOutput.println("<STYLE>");
      webPageOutput.println(".highlight {background-color:#000478;}");
      webPageOutput.println(".redzone   {background-color:#c90000;}");
      webPageOutput.println(".home      {background-color:#" + homeTeamShirtColour + ";color:#" + homeTeamNumberColour + ";}");
      webPageOutput.println(".away      {background-color:#" + awayTeamShirtColour + ";color:#" + awayTeamNumberColour + ";}");
      webPageOutput.println("</STYLE>");
      webPageOutput.println("</HEAD>");
      webPageOutput.println("<BODY BGCOLOR=\"#ffffff\" LEFTMARGIN=\"0\" MARGINWIDTH=\"0\" TOPMARGIN=\"0\" MARGINHEIGHT=\"0\" TEXT=\"#000000\" LINK=\"#000a78\">");
      webPageOutput.println("<TABLE WIDTH=\"800\" CELLPADDING=\"0\" CELLSPACING=\"0\" BORDER=\"0\">");
      webPageOutput.println("<TBODY>");
      webPageOutput.println("<TR VALIGN=\"top\">");
      webPageOutput.println("<TD WIDTH=\"130\" ALIGN=\"center\" BGCOLOR=\"#000000\">");
      webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"0\" BORDER=\"0\" BGCOLOR=\"#5961a6\">");
      webPageOutput.println("<TBODY>");
      webPageOutput.print("<TR><TD ALIGN=\"center\" BGCOLOR=\"#000000\">");
      WriteHTMLLink(request,
                    response,
                    webPageOutput,
                    "Main",
                    null,
                    "",
                    null,
                    false);
      webPageOutput.println("<IMG SRC=\"../Images/ClubPitLogo.jpg\" WIDTH=\"103\" HEIGHT=\"25\" ALT=\"Click here to return to main screen\" BORDER=\"0\"></A></TD></TR>");
      if("test".equals(context.getInitParameter("server")))
        {
        webPageOutput.println("<TR><TD ALIGN=\"center\" BGCOLOR=\"#000000\"><A HREF=\"http://sourceforge.net/projects/openfootball/\"><IMG SRC=\"../Images/SourceForge.bmp\" WIDTH=\"125\" HEIGHT=\"37\" BORDER=\"0\" ALT=\"SourceForge.net Logo\" /></A></TD></TR>");
        }
      else
        {
        webPageOutput.println("<TR><TD ALIGN=\"center\" BGCOLOR=\"#000000\"><A HREF=\"http://sourceforge.net/projects/openfootball/\"><IMG SRC=\"http://sourceforge.net/sflogo.php?group_id=79566&amp;type=3\" WIDTH=\"125\" HEIGHT=\"37\" BORDER=\"0\" ALT=\"SourceForge.net Logo\" /></A></TD></TR>");
        }
      webPageOutput.println("<TR><TD WIDTH=\"2\" BGCOLOR=\"black\"><SPACER TYPE=\"block\" WIDTH=\"1\"></TD></TR>");
      webPageOutput.println("<TR><TD HEIGHT=\"1\" BGCOLOR=\"#000000\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");

      //Side Bar Menu
      //Sign On/SignOff Button
      if (menuHighLight == 8)
         {
         webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
         }
      else
         {
         webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
         }
      webPageOutput.print(Routines.indent(5));
      if (coachNumber!=0)
         {
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "Exit",
                       null,
                       "SIGN OUT",
                       null,
                       true);
         }
      else
         {
         if (selectedLeague!=0)
            {
            WriteHTMLLink(request,response,
                          webPageOutput,
                          "SignIn",
                          "league=" +
                          selectedLeague +
                          "&season=" +
                          selectedSeason +
                          "&week=" +
                          selectedWeek,
                          "SIGN IN",
                          null,
                          true);
            }
         else
            {
            WriteHTMLLink(request,response,
                          webPageOutput,
                          "SignIn",
                          null,
                          "SIGN IN",
                          null,
                          true);
            }
         }
      webPageOutput.println("</LI></TD></TR>");
      webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");

      //MyTeam Button
      int myTeamNumber=0;
      if(coachNumber!=0)
        {
        myTeamNumber=Routines.getMyTeam(request,selectedLeague,database,context);
        if(myTeamNumber!=0)
         {
         if(menuHighLight == 9)
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
           }
         else
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
           }
         webPageOutput.print(Routines.indent(5));
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "MyTeam",
                       "league=" +
                       selectedLeague +
                       "&team=" +
                       myTeamNumber,
                       "MY TEAM",
                       null,
                       true);
         webPageOutput.println("</LI></TD></TR>");
         webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");
         }
       }

      //MyAccount/SignUp Button
      if (menuHighLight == 10)
         {
         webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
         }
      else
         {
         webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
         }
      webPageOutput.print(Routines.indent(5));
      if (coachNumber!=0)
         {
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=myAccount",
                       "MY ACCOUNT",
                       null,
                       true);
         }
      else
         {
         if(selectedLeague!=0)
           {
           WriteHTMLLink(request,response,
                         webPageOutput,
                         "CoachMaintenance",
                         "league=" +
                         selectedLeague,
                         "SIGN UP",
                         null,
                         true);
           }
         else
           {
           WriteHTMLLink(request,response,
                         webPageOutput,
                         "CoachMaintenance",
                         null,
                         "SIGN UP",
                         null,
                         true);
           }
         }
      webPageOutput.println("</LI></TD></TR>");
      webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");

      //MainAdmin Button
      boolean administrator=false;
      String tempAdmin="";
      if(session!=null)
        {
        tempAdmin=(String)session.getAttribute("administrator");
        }
      if("true".equals(tempAdmin))
        {
        administrator=true;
        }
      if(administrator)
         {
         if(menuHighLight == 13)
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
           }
         else
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
           }
         webPageOutput.print(Routines.indent(5));
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "MainAdmin",
                       null,
                       "MAIN ADMIN",
                       null,
                       true);
         webPageOutput.println("</LI></TD></TR>");
         webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");
         if(menuHighLight == 11)
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
           }
         else
           {
           webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
           }
         webPageOutput.print(Routines.indent(5));
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "GameAdmin",
                       null,
                       "GAME ADMIN",
                       null,
                       true);
         webPageOutput.println("</LI></TD></TR>");
         webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");
         }

      //League Admin Button
      if(administrator&&selectedLeague!=0)
         {
         if (menuHighLight == 12)
            {
            webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
            }
         else
            {
            webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
            }
         webPageOutput.print(Routines.indent(5));
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "LeagueAdmin",
                       "league=" +
                       selectedLeague,
                       "LEAGUE ADMIN",
                       null,
                       true);
         webPageOutput.println("</LI></TD></TR>");
         webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");
         }
      //Button for each League
      webPageOutput.println("<TR><TD HEIGHT=\"17\" CLASS=\"lnHdr\" ALIGN=\"center\" BGCOLOR=\"#000000\"><BR>LEAGUES</TD></TR>");
      webPageOutput.println("<TR><TD HEIGHT=\"1\" BGCOLOR=\"#000000\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");

      try
         {
         if(leaguesFound)
           {
           queryResult.beforeFirst();
           while(queryResult.next())
             {
             String leagueName=new String(queryResult.getString(1));
             int leagueNumber=queryResult.getInt(2);
             if(selectedLeague==leagueNumber)
               {
               webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln2\">");
               }
             else
               {
               webPageOutput.print("<TR><TD HEIGHT=\"18\" CLASS=\"ln\">");
               }
             webPageOutput.print(Routines.indent(5));
             WriteHTMLLink(request,response,
                           webPageOutput,
                           "Scores",
                           "league=" +
                           leagueNumber,
                           leagueName,
                           null,
                           true);
             webPageOutput.println("</LI></TD></TR>");
             webPageOutput.println("<TR><TD HEIGHT=\"1\"><SPACER TYPE=\"BLOCK\" WIDTH=\"1\" HEIGHT=\"1\"></TD></TR>");
             }
           }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving leagues: " + error,false,context);	
         }

      webPageOutput.println("</TBODY>");
      webPageOutput.println("</TABLE>");
      webPageOutput.println("</TD>");
      webPageOutput.println("<TD WIDTH=\"2\" BGCOLOR=\"black\"><SPACER TYPE=\"block\" WIDTH=\"1\"></TD>");
      webPageOutput.println("<TD WIDTH=\"668\">");
      webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPASSING=\"0\" CELLSPACING=\"0\" BORDER=\"0\" BGCOLOR=\"white\">");
      webPageOutput.println("<TBODY>");
      webPageOutput.println("<TR WIDTH=\"100%\" ALIGN=\"center\">");

      //Top Bar Menu
      if (menu)
         {
         highLightMenu(1,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewNews" +
                       "&league=" +
                       selectedLeague,
                       "NEWS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(2,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "Scores",
                       "league=" +
                       selectedLeague,
                       "SCORES",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(3,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewStats" +
                       "&league=" +
                       selectedLeague,
                       "STATS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(4,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "Schedule",
                       "league=" +
                       selectedLeague,
                       "SCHEDULES",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(5,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "Standings",
                       "league=" +
                       selectedLeague,
                       "STANDINGS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(6,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewTeams" +
                       "&league=" +
                       selectedLeague,
                       "TEAMS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(7,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewPlayers" +
                       "&league=" +
                       selectedLeague,
                       "PLAYERS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(14,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "Drafts",
                       "league=" +
                       selectedLeague,
                       "DRAFTS",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(15,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewPlayers" +
                       "&league=" +
                       selectedLeague,
                       "CUP",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(16,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewPlayers" +
                       "&league=" +
                       selectedLeague,
                       "FAME",
                       null,
                       true);
         webPageOutput.println("</TD>");
         menuSpacer(webPageOutput);
         highLightMenu(17,menuHighLight,webPageOutput);
         WriteHTMLLink(request,response,
                       webPageOutput,
                       "wfafl",
                       "action=viewPlayers" +
                       "&league=" +
                       selectedLeague,
                       "FORUM",
                       null,
                       true);
         webPageOutput.println("</TD>");
         }
      else
         {
         webPageOutput.print("<TD HEIGHT=\"20\" CLASS=\"ln3\" ALIGN=\"Center\">");
         if(title!=null)
           {
           webPageOutput.println(title);
           }
         webPageOutput.println("</TD>");
         }
      webPageOutput.println("</TR>");
      webPageOutput.println("</TBODY>");
      webPageOutput.println("</TABLE>");
      webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPASSING=\"0\" CELLSPACING=\"0\" BORDER=\"0\" BGCOLOR=\"white\">");
      webPageOutput.println("<TBODY>");
      webPageOutput.println("<TR WIDTH=\"100%\">");
      webPageOutput.println("<TD WIDTH=\"2%\" BGCOLOR=\"#ffffff\"></TD>");
      webPageOutput.println("<TD WIDTH=\"96%\">");
      if((scores||standings)&&selectedLeagueType==2)
        {
		webPageOutput.println("<IMG SRC=\"../Images/UNFL.png\" WIDTH=\"660\" HEIGHT=\"106\" ALT=\"UNFL\" BORDER=\"0\"></A>");
        }
      //Season/Weeks Selection
      if(seasonsMenu||weeksMenu)
         {
         if(scores)
            {
            action="Scores";
            }
         if(standings)
            {
            action="Standings";
            }
         if(schedules)
            {
            action="Schedule";
            }
         if(draft)
            {
            action="Draft";	   
            }
         if(seasonsMenu&&selectedLatestSeason>1)
            {
            //Seasons
            webPageOutput.println("<FONT CLASS=\"opt2\">");
            webPageOutput.println("<B>Season</B>:");
            webPageOutput.println("</FONT>");
            for (int season=1;season<=selectedLatestSeason;season++)
               {
               String style = "";
               if (season==selectedSeason)
                  {
                  style="optsel";
                  }
               else
                  {
                  style="opt2";
                  }
               WriteHTMLLink(request,response,
                             webPageOutput,
                             action,
                             "league=" +
                             selectedLeague +
                             "&season=" +
                             season +
                             "&week=1",
                             String.valueOf(season),
                             style,
                             true);
               if (season<selectedLatestSeason)
                  {
                  webPageOutput.println("<B>·</B>");
                  }
               }
            webPageOutput.println("<BR>");
            }
         int maxPreSeasonWeeks=0;
         int maxRegularSeasonWeeks=0;
         if(scores)
            {
            maxPreSeasonWeeks=selectedLeaguePreSeasonWeeks;
            maxRegularSeasonWeeks=selectedLeagueRegularSeasonWeeks;
            }
         if(standings)
            {
            if(selectedSeason<selectedLatestSeason)
               {
               maxPreSeasonWeeks=selectedLeaguePreSeasonWeeks;
               maxRegularSeasonWeeks=selectedLeagueRegularSeasonWeeks;
               }
            else
               {
               if(selectedLatestWeek>selectedLeaguePreSeasonWeeks)
                  {
                  maxPreSeasonWeeks=selectedLeaguePreSeasonWeeks;
                  }
               else
                  {
                  maxPreSeasonWeeks=selectedLatestWeek;
                  }
               if(selectedLatestWeek>
                  (selectedLeaguePreSeasonWeeks + selectedLeagueRegularSeasonWeeks))
                  {
                  maxRegularSeasonWeeks=selectedLeagueRegularSeasonWeeks;
                  }
               else
                  {
                  if(selectedLatestWeek>selectedLeaguePreSeasonWeeks)
                     {
                     maxRegularSeasonWeeks=selectedLatestWeek -
                                           selectedLeaguePreSeasonWeeks;
                     }
                  else
                     {
                     maxRegularSeasonWeeks=0;
                     }
                  }
               }
            }
         //Pre-Season Weeks
         if(weeksMenu)
           {
           if(standings&&selectedLatestSeason==selectedSeason&&selectedLatestWeek==0)
              {
              }
           else
              {
              webPageOutput.println("<FONT CLASS=\"opt2\">");
              webPageOutput.println("<B>Preseason Week</B>:");
              webPageOutput.println("</FONT>");
              }
           for(int preSeason=1;preSeason<=maxPreSeasonWeeks;preSeason++)
              {
              String style = "";
              if(preSeason==selectedWeek||(preSeason==1 && selectedWeek==0))
                 {
                 style="optsel";
                 }
              else
                 {
                 style="opt2";
                 }
              if(schedules)
                 {
                 webPageOutput.print( "<A ");
                 webPageOutput.print("CLASS = \"" + style + "\" ");
                 webPageOutput.println("HREF=\"#Week" +
                                       preSeason +
                                       "\">" +
                                       String.valueOf(preSeason));
                 webPageOutput.println("</A>");
                 }
              else
                 {
                 WriteHTMLLink(request,response,
                               webPageOutput,
                               action,
                               "league=" +
                               selectedLeague +
                               "&season=" +
                               selectedSeason +
                               "&week=" +
                               preSeason,
                               String.valueOf(preSeason),
                               style,
                               true);
                 }
              if (preSeason < maxPreSeasonWeeks)
                 {
                 webPageOutput.println("<B>·</B>");
                 }
              }
           // Regular Season Weeks
           if(standings&&selectedLatestSeason==selectedSeason&&
              (selectedLatestWeek<=selectedLeaguePreSeasonWeeks))
              {
              }
           else
              {
              webPageOutput.println("<BR>");
              webPageOutput.println("<FONT CLASS=\"opt2\">");
              webPageOutput.println("<B>Regular Season Week</B>:");
              webPageOutput.println("</FONT>");
              }
           for (int regularSeason=1;regularSeason<=maxRegularSeasonWeeks;regularSeason++)
              {
              String style = "";
              if (selectedLeaguePreSeasonWeeks + regularSeason==selectedWeek)
                 {
                 style="optsel";
                 }
              else
                 {
                 style="opt2";
                 }
              if(schedules)
                {
                webPageOutput.print( "<A ");
                webPageOutput.print("CLASS = \"" + style + "\" ");
                webPageOutput.println("HREF=\"#Week" +
                                      (regularSeason + selectedLeaguePreSeasonWeeks) +
                                      "\">" +
                                      String.valueOf(regularSeason));
                webPageOutput.println("</A>");
                }
              else
                {
                WriteHTMLLink(request,response,
                              webPageOutput,
                              action,
                              "league=" +
                              selectedLeague +
                              "&season=" +
                              selectedSeason +
                              "&week=" +
                              (regularSeason + selectedLeaguePreSeasonWeeks),
                              String.valueOf(regularSeason),
                              style,
                              true);
                }
              if (regularSeason < maxRegularSeasonWeeks)
                 {
                 webPageOutput.println("<B>·</B>");
                 }
              }
           // Post Season Weeks
           if(scores)
              {
              if (selectedSeason<selectedLatestSeason ||
                  selectedLatestWeek >= (selectedLeaguePreSeasonWeeks + selectedLeagueRegularSeasonWeeks))
                 {
                 webPageOutput.println("<BR>");
                 webPageOutput.println("<FONT CLASS=\"opt2\">");
                 webPageOutput.println("<B>Post Season</B>:");
                 webPageOutput.println("</FONT>");
                 int numOfPostSeasonWeeks;
                 if(selectedSeason<selectedLatestSeason)
                    {
                    numOfPostSeasonWeeks = selectedLeaguePostSeasonWeeks;
                    }
                 else
                    {
                    numOfPostSeasonWeeks = selectedLeaguePostSeasonWeeks - ((selectedLeaguePreSeasonWeeks +
                                                                             selectedLeagueRegularSeasonWeeks +
                                                                             selectedLeaguePostSeasonWeeks) -
                                                                             selectedLatestWeek);
                    }
                 for (int postSeason=1;postSeason<=(numOfPostSeasonWeeks - 1);postSeason++)
                    {
                    String style = "";
                    if (selectedLeaguePreSeasonWeeks +
                        selectedLeagueRegularSeasonWeeks +
                        postSeason==selectedWeek)
                       {
                       style="optsel";
                       }
                    else
                       {
                       style="opt2";
                       }
                    if(schedules)
                      {
                      webPageOutput.print( "<A ");
                      webPageOutput.print("CLASS = \"" + style + "\" ");
                      webPageOutput.println("HREF=\"#Week" +
                                            (selectedLeaguePreSeasonWeeks +
                                            selectedLeagueRegularSeasonWeeks +
                                            postSeason) +
                                            "\">" +
                                            Routines.decodeWeekNumber(selectedLeaguePreSeasonWeeks,
                                                                     selectedLeagueRegularSeasonWeeks,
                                                                     selectedLeaguePostSeasonWeeks,
                                                                     selectedSeason,
                                                                     selectedLeaguePreSeasonWeeks +
                                                                     selectedLeagueRegularSeasonWeeks +
                                                                     postSeason,
                                                                     false,
                                                                     session));
                      webPageOutput.println("</A>");
                      }
                    else
                      {
                      WriteHTMLLink(request,response,
                                    webPageOutput,
                                    "Scores",
                                    "league=" +
                                    selectedLeague +
                                    "&season=" +
                                    selectedSeason +
                                    "&week=" +
                                    (selectedLeaguePreSeasonWeeks +
                                    selectedLeagueRegularSeasonWeeks +
                                    postSeason),
                                    Routines.decodeWeekNumber(selectedLeaguePreSeasonWeeks,
                                                              selectedLeagueRegularSeasonWeeks,
                                                              selectedLeaguePostSeasonWeeks,
                                                              selectedSeason,
                                                              selectedLeaguePreSeasonWeeks +
                                                              selectedLeagueRegularSeasonWeeks +
                                                              postSeason,
                                                              false,
                                                              session),
                                    style,
                                    true);
                      }
                    webPageOutput.println("<B>·</B>");
                    }
                 WriteHTMLLink(request,response,
                               webPageOutput,
                               "Scores",
                               "league=" +
                               selectedLeague +
                               "&season=" +
                               selectedSeason +
                               "&week=" +
                               (selectedLeaguePreSeasonWeeks +
                                selectedLeagueRegularSeasonWeeks +
                                numOfPostSeasonWeeks),
                               Routines.decodeWeekNumber(selectedLeaguePreSeasonWeeks,
                                                         selectedLeagueRegularSeasonWeeks,
                                                         selectedLeaguePostSeasonWeeks,
                                                         selectedSeason,
                                                         selectedLeaguePreSeasonWeeks +
                                                         selectedLeagueRegularSeasonWeeks +
                                                         numOfPostSeasonWeeks,
                                                         false,
                                                         session),
                               "opt2",
                               true);
                 }
              }
           dotSpacer(webPageOutput);
           }
         }  
      if(gameCenter)
         {
         titleHeader("head2",true,webPageOutput);
         webPageOutput.println("Season " +
                               selectedSeason +
                               ", " +
                               Routines.decodeWeekNumber(selectedLeaguePreSeasonWeeks,
                                                                selectedLeagueRegularSeasonWeeks,
                                                                selectedLeaguePostSeasonWeeks,
                                                                selectedSeason,
                                                                selectedWeek,
                                                                false,
                                                                session));
         webPageOutput.println(" - " + awayTeamName + " @ " + homeTeamName);
         titleTrailer(true,webPageOutput);
         webPageOutput.println("<CENTER>");
         dotSpacer(webPageOutput);
         webPageOutput.println("<IMG SRC=\"../Images/GameCenter_PlayByPlay.gif\" HEIGHT=\"30\" WIDTH=\"356\" BORDER=\"0\" ALT=\"GameCenter : Play-by-Play\">");
         webPageOutput.println(Routines.spaceLines(1));
         webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Recap</A>");
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Game Stats</A>");
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"optsel\" HREF=\"\">Play by Play</A>");
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Drive Charts</A>");
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Gamebook</A>");
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Game Viewer</A>");
         dotSpacer(webPageOutput);
         webPageOutput.println(Routines.spaceLines(1));
         webPageOutput.println("</CENTER>");
         }
      if(preview)
        {
        String previewMenuText="Inter";
        try
          {
          Statement sql = database.createStatement();
          ResultSet queryResponse;
          queryResult = sql.executeQuery("SELECT divisions.DivisionNumber," +
                                         "conferences.ConferenceNumber " +
                                         "FROM leagueteams," +
                                         "divisions," +
                                         "conferences " +
                                         "WHERE TeamNumber IN (" + (String)session.getAttribute("homeTeamNumber") + "," +
                                         (String)session.getAttribute("awayTeamNumber") + ") " +
                                         "AND   leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                         "AND   divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                         "AND   conferences.LeagueNumber   = " + (String)session.getAttribute("league"));
          int division1=0;
          int division2=0;
          int conference1=0;
          int conference2=0;
          queryResult.first();
          division1=queryResult.getInt(1);
          conference1=queryResult.getInt(2);
          queryResult.next();
          division2=queryResult.getInt(1);
          conference2=queryResult.getInt(2);
          if(division1==division2)
            {
            previewMenuText="Division";
            }
          else
            {
            if(conference1==conference2)
              {
              previewMenuText="Conference";
              }
            }
          }
        catch(SQLException error)
          {
	      Routines.writeToLog(servletName,"Database error retrieving Preview Title data (WebPageOutput.WriteHTMLHead) : " + error,false,context);	
          }
        titleHeader("head2",true,webPageOutput);
        webPageOutput.println("Season " +
                              selectedSeason +
                              ", " +
                              Routines.decodeWeekNumber(selectedLeaguePreSeasonWeeks,
                                                               selectedLeagueRegularSeasonWeeks,
                                                               selectedLeaguePostSeasonWeeks,
                                                               selectedSeason,
                                                               selectedWeek,
                                                               false,
                                                               session));
        webPageOutput.println(" - " + awayTeamName + " @ " + homeTeamName);
        titleTrailer(true,webPageOutput);
        webPageOutput.println("<CENTER>");
        dotSpacer(webPageOutput);
        webPageOutput.println("<IMG SRC=\"../Images/GameCenter_Preview.gif\" HEIGHT=\"30\" WIDTH=\"291\" BORDER=\"0\" ALT=\"GameCenter : Play-by-Play\">");
        webPageOutput.println(Routines.spaceLines(1));
        webPageOutput.println("<A CLASS=\"optsel\" HREF=\"\">Overview</A>");
        webPageOutput.println("<B>·</B>");
        webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Overall</A>");
        webPageOutput.println("<B>·</B>");
        webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Head to Head</A>");
        webPageOutput.println("<B>·</B>");
        webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">Home/Away</A>");
        webPageOutput.println("<B>·</B>");
        webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">After Win/Loss</A>");
        webPageOutput.println("<B>·</B>");
        webPageOutput.println("<A CLASS=\"opt\" HREF=\"\">" + previewMenuText + "<A>");
        dotSpacer(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        webPageOutput.println("</CENTER>");
        }
      if(teamCenter)
         {
         String coachName="Unknown";
         boolean autoCoach=false;
         try
           {
           Statement sql = database.createStatement();
           ResultSet queryResponse;
           queryResult = sql.executeQuery("SELECT Forenames,Surname,AutoCoach " +
                                          "FROM coaches,coachteams " +
                                          "WHERE TeamNumber=" + teamNumber + " " +
                                          "AND coachteams.CoachNumber=coaches.CoachNumber");
           if(queryResult.first())
             {
             coachName=queryResult.getString(1)+" "+queryResult.getString(2);
             if(queryResult.getInt(3)==1)
               {
               autoCoach=true;
               }
             }
           }
         catch(SQLException error)
           {
           Routines.writeToLog(servletName,"Error getting coach : " + error,false,context);
           }
         if(selectedLeagueType==2)
           {
           String teamName=(String)session.getAttribute("teamName");	
           if(teamName.equals("Chicago Penguins"))	
             {
			 webPageOutput.println("<IMG SRC=\"../Images/ChicagoPenguins.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Chicago Penguins\" BORDER=\"0\"></A>");
      	   	 }
		   if(teamName.equals("Orlando Magic"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/OrlandoMagic.png\" WIDTH=\"650\" HEIGHT=\"148\" ALT=\"Orlando Magic\" BORDER=\"0\"></A>");
			 } 
		   if(teamName.equals("Munich Versaces"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/MunichVersaces.png\" WIDTH=\"650\" HEIGHT=\"180\" ALT=\"Munich Versaces\" BORDER=\"0\"></A>");
			 }
		   if(teamName.equals("Kansas Coyotes"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/KansasCoyotes.png\" WIDTH=\"650\" HEIGHT=\"157\" ALT=\"Kansas Coyotes\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Bayside Sharks"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/BaysideSharks.png\" WIDTH=\"650\" HEIGHT=\"140\" ALT=\"Bayside Sharks\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Cincinnati Bengals"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/CincinnatiBengals.png\" WIDTH=\"650\" HEIGHT=\"170\" ALT=\"Cincinnati Bengals\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Northampton Stormbringers"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/NorthamptonStormbringers.png\" WIDTH=\"650\" HEIGHT=\"148\" ALT=\"Northampton Stormbringers\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Edinburgh Hurricanes"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/EdinburghHurricanes.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Edinburgh Hurricanes\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Colorado White Tigers"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/ColoradoWhiteTigers.png\" WIDTH=\"650\" HEIGHT=\"155\" ALT=\"Colorado White Tigers\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Purdue Rams"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/PurdueRams.png\" WIDTH=\"650\" HEIGHT=\"200\" ALT=\"Purdue Rams\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Boston Bullets"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/BostonBullets.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Boston Bullets\" BORDER=\"0\"></A>");
			 }
		   if(teamName.equals("Berlin King Squad"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/BerlinKingSquad.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Berlin King Squad\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Iowa Hawks"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/IowaHawks.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Iowa Hawks\" BORDER=\"0\"></A>");
			 }		
		   if(teamName.equals("Montreal Mounties"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/MontrealMounties.png\" WIDTH=\"650\" HEIGHT=\"180\" ALT=\"Montreal Mounties\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Staines Removers"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/StainesRemovers.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Staines Removers\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Lord Howe Mutton Birds"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/LordHoweMuttonBirds.png\" WIDTH=\"650\" HEIGHT=\"140\" ALT=\"Lord Howe Mutton Birds\" BORDER=\"0\"></A>");
			 }
		   if(teamName.equals("San Francisco Jedi"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/SanFranciscoJedi.png\" WIDTH=\"650\" HEIGHT=\"160\" ALT=\"San Francisco Jedi\" BORDER=\"0\"></A>");
			 }	
		   if(teamName.equals("Tyrone Tigers"))	
			 {
			 webPageOutput.println("<IMG SRC=\"../Images/TyroneTigers.png\" WIDTH=\"650\" HEIGHT=\"150\" ALT=\"Tyrone Tigers\" BORDER=\"0\"></A>");
			 }	  			 			 			 		 		 			 			 	 			 			 			 		  	      	   	 
		   }
        if(selectedLeagueType==2)
	       {	
		   webPageOutput.println("<DIV CLASS=\"head2ul\">");
	       }
	     else
	       {
		   webPageOutput.println("<DIV CLASS=\"TeamTable\"><TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
		   webPageOutput.println("<TBODY>");
		   webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
	       webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"8\">");
	       }
         webPageOutput.println("<CENTER>");
         if(selectedLeagueType!=2)
           {
           webPageOutput.println("<FONT CLASS=\"home\">");	
           }
         if(selectedLeagueType!=2)
           {
           webPageOutput.println((String)session.getAttribute("teamName")+" (");
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "CoachInfo",
								  "league=" +
								  selectedLeague +
								  "&coach=" +
								  coachNumber,
								  coachName,
								  "TeamTableCoachLink",
								  true);
           }
         else
           { 
		   webPageOutput.println("HeadCoach ");	 
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "CoachInfo",
								  "league=" +
								  selectedLeague +
								  "&coach=" +
								  coachNumber,
								  coachName,
			                      null,
								  true);
		   webPageOutput.println("<BR>");						  
           }
		 if(selectedLeagueType!=2)
		   {
		   webPageOutput.println(")");	
		   webPageOutput.println("</FONT>"); 
		   }
         webPageOutput.println(Routines.spaceLines(1));
         if(autoCoach)
           {
           int disabled=Routines.getMyTeam(request,selectedLeague,database,context);
           String disabledText="";
           if(!administrator&&(disabled!=0||!allowCoachChanges))
             {
             disabledText=" DISABLED";
             }
           webPageOutput.println("<FORM ACTION=\"http://" +
                                 request.getServerName() +
                                 ":" +
                                 request.getServerPort() +
                                 request.getContextPath() +
                                 "/servlet/TeamNews\" METHOD=\"GET\">");
           webPageOutput.println("<INPUT" + disabledText + " TYPE=\"SUBMIT\" VALUE=\"Coach This Team\" NAME=\"action\">");
           webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
           webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + selectedLeague + "\">");
           webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"selectedTeam\" VALUE=\"" + teamNumber + "\">");
           webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + teamNumber + "\">");
           webPageOutput.println("</FORM>");
           webPageOutput.println(Routines.spaceLines(1));
           }
         String linkClass="";  
		 if(title.equals("Team News"))
		   {
		   linkClass="optselwhite";	
		   if(selectedLeagueType!=2)
			 {
			 webPageOutput.println("<FONT CLASS=\""+linkClass+"\">");
			 }
		   webPageOutput.println("News");
		   if(selectedLeagueType!=2)
			 {
			 webPageOutput.println("</FONT");
			 }
		   }
		 else
		   { 
		   linkClass="optwhite";
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "TeamNews",
								  "league=" +
								  selectedLeague +
								  "&coach=" +
								  coachNumber +
								  "&team=" +
								  teamNumber,
								  "News",
								  linkClass,
								  true);
		   }
		 webPageOutput.println("<B>·</B>");
		 if(title.equals("Roster"))
		   {
		   linkClass="optselwhite";
		   if(selectedLeagueType!=2)
		     {
		     webPageOutput.println("<FONT CLASS=\""+linkClass+"\">");
		     }
		   webPageOutput.println("Roster");
		   if(selectedLeagueType!=2)
			 {
		     webPageOutput.println("</FONT");
			 }
		   }
		 else
		   { 
		   linkClass="optwhite";
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "league=" +
								  selectedLeague +
								  "&coach=" +
								  coachNumber +
								  "&team=" +
								  teamNumber,
								  "Roster",
								  linkClass,
								  true);
         }						  		 
         webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"optwhite\" HREF=\"\">Depth Chart</A>");
		 webPageOutput.println("<B>·</B>");
         webPageOutput.println("<A CLASS=\"optwhite\" HREF=\"\">Team Stats</A>");
		 webPageOutput.println("<B>·</B>");
         if(title.equals("Team Schedule"))
           {
           linkClass="optselwhite";	
		   if(selectedLeagueType!=2)
			 {
			 webPageOutput.println("<FONT CLASS=\""+linkClass+"\">");
			 }
		   webPageOutput.println("Schedule");
		   if(selectedLeagueType!=2)
			 {
			 webPageOutput.println("</FONT");
			 }
           }
		 else
		   { 
		   linkClass="optwhite";
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "TeamSchedule",
								  "league=" +
								  selectedLeague +
								  "&coach=" +
								  coachNumber +
								  "&team=" +
								  teamNumber,
								  "Schedule",
								  linkClass,
								  true);
		   }
         webPageOutput.println(Routines.spaceLines(1));
         webPageOutput.println("</CENTER>");
         if(selectedLeagueType!=2)
           {
		   webPageOutput.println("</TD>");
		   webPageOutput.println("</TR>");
		   webPageOutput.println("</TBODY>");
		   webPageOutput.println("</TABLE>");
           }
         webPageOutput.println("</DIV>");
         }
      }

  static void WriteHTMLTail(HttpServletRequest  request,
                            HttpServletResponse response,
                            PrintWriter webPageOutput)
     {
     int league=Routines.safeParseInt(request.getParameter("league"));
     webPageOutput.println("</TD>");
     webPageOutput.println("<TD WIDTH=\"2%\" BGCOLOR=\"#ffffff\"></TD>");
     webPageOutput.println("</TR>");
     webPageOutput.println("</TBODY>");
     webPageOutput.println("</TABLE>");
     webPageOutput.println("</TD>");
     webPageOutput.println("</TR>");
     webPageOutput.println("<TABLE WIDTH=\"800\" CELLPADDING=\"0\" CELLSPACING=\"0\" BORDER=\"0\" BGCOLOR=\"#000000\">");
     webPageOutput.println("<TBODY>");
     webPageOutput.println("<TR><TD COLSPAN=\"2\" WIDTH=\"800\" HEIGHT=\"3\"><SPACER TYPE=\"block\" WIDTH=\"800\" HEIGHT=\"3\"></TD></TR>");
     webPageOutput.println("<TR>");
     webPageOutput.println("<TD WIDTH=\"1\" HEIGHT=\"1\"><SPACER TYPE=\"block\" WIDTH=\"1\" HEIGHT=\"1\"></TD>");
     webPageOutput.println("<TD VALIGN=\"top\">");
     webPageOutput.println("<FONT CLASS=\"white\">");
     if(league!=0)
       {
       WriteHTMLLink(request,response,
                     webPageOutput,
                    "Contact",
                     "league=" +
                     league,
                     "Contact Us",
                     null,
                     true);
       webPageOutput.println(" | ");
       WriteHTMLLink(request,response,
                     webPageOutput,
                    "FAQ",
                     "league=" +
                     league,
                     "FAQ",
                     null,
                     true);
       webPageOutput.println(" | ");
       WriteHTMLLink(request,response,
                     webPageOutput,
                     "Policies",
                     "league=" +
                     league,
                     "Policies",
                     null,
                     true);
       }
     else
       {
       WriteHTMLLink(request,response,
                     webPageOutput,
                    "Contact",
                     null,
                     "Contact Us",
                     null,
                     true);
       webPageOutput.println(" | ");
       WriteHTMLLink(request,response,
                     webPageOutput,
                    "FAQ",
                     null,
                     "FAQ",
                     null,
                     true);
       webPageOutput.println(" | ");
       WriteHTMLLink(request,response,
                     webPageOutput,
                     "Policies",
                     null,
                     "Policies",
                     null,
                     true);
       }
     webPageOutput.println("<BR>");
     webPageOutput.println("<BR>");
     webPageOutput.println("</FONT>");
     webPageOutput.println("</TD>");
     webPageOutput.println("</TR>");
     webPageOutput.println("</TBODY>");
     webPageOutput.println("</TABLE>");
     webPageOutput.println("</BODY>");
     webPageOutput.println("</HTML>");
     }

  static void WriteHTMLLink(HttpServletRequest request,
                            HttpServletResponse response,
                            PrintWriter webPageOutput,
                            String servlet,
                            String params,
                            String text,
                            String type,
                            boolean close)
     {
     String encodedURL=Routines.encodeURL(request,response,servlet,params);
     webPageOutput.print( "<A ");
     if (type != null)
        {
        webPageOutput.print("CLASS = \"" + type + "\" ");
        }
     webPageOutput.print("HREF=\"" + encodedURL + "\">" + text);
     if(close)
       {
       webPageOutput.println("</A>");
       }
     }
     
  static void writeExternalHTMLLink(PrintWriter webPageOutput,
							          String url,
							          String text,
							          String type,
							          boolean close)
	   {
	   webPageOutput.print("<A TARGET=\"_blank\" ");
	   if(type!=null)
		  {
		  webPageOutput.print("CLASS = \""+type+"\" ");
		  }
	   webPageOutput.print("HREF=\"" + url + "\">" + text);
	   if(close)
		 {
		 webPageOutput.println("</A>");
		 }
	   }     

  static String encodeURL(HttpServletRequest request,
                          HttpServletResponse response,
                          String servlet,
                          String params)
     {
     String uncodedURL;
     if(params==null)
       {
       uncodedURL="http://" +
                  request.getServerName() +
                  ":" +
                  request.getServerPort() +
                  request.getContextPath() +
                  "/servlet/" +
                  servlet;
       }
     else
       {
       uncodedURL="http://" +
                  request.getServerName() +
                  ":" +
                  request.getServerPort() +
                  request.getContextPath() +
                  "/servlet/" +
                  servlet +
                  "?" +
                  params;
       }
     String encodedURL=response.encodeURL(uncodedURL);
     return encodedURL;
     }


  static void titleHeader(String      type,
                          boolean     center,
                          PrintWriter webPageOutput)
     {
     webPageOutput.println("<TABLE CELLSPACING=\"0\" CELLPADDING=\"0\" WIDTH=\"100%\" BORDER=\"0\" BGCOLOR=\"white\">");
     webPageOutput.println("<TBODY>");
     webPageOutput.println("<TR VALIGN=\"top\">");
     webPageOutput.println("<TD CLASS=\"" +
                           type +
                           "\">");
     if(center)
       {
       webPageOutput.println("<CENTER>");
       }
     }

  static void titleTrailer(boolean center,
                           PrintWriter webPageOutput)
     {
     if(center)
       {
       webPageOutput.println("</CENTER>");
       }
     webPageOutput.println("</TD>");
     webPageOutput.println("</TR>");
     webPageOutput.println("</TBODY>");
     webPageOutput.println("</TABLE>");
     webPageOutput.println("<P></P>");
     }

  static void highLightMenu(int menuItem,
                            int highLightItem,
                            PrintWriter webPageOutput)
       {
       if (menuItem == highLightItem)
          {
          webPageOutput.println("   <TD HEIGHT=\"20\" CLASS=\"lnOn\">");
          }
       else
          {
          webPageOutput.println("   <TD HEIGHT=\"20\" CLASS=\"ln\">");
          }
       }

  static int howGoodAmI(int playerNumber,
                        int[] skills,
                        int numOfSkills)
       {
       int returnInt=0;
       returnInt=Routines.getSkillRating(numOfSkills,skills);	
       return returnInt;
       }
       
  static int[] whatLeagueAmI(int teamNumber,
                             Connection database,
	                         ServletContext context)
		 {
		 String servletName="Routines.whatLeagueAmI";	
		 int[] returnInt=new int[2];
		 try
			{
			Statement sql=database.createStatement();
			ResultSet queryResult=sql.executeQuery("SELECT leagues.LeagueNumber,LeagueType " +
											       "FROM   leagueteams,divisions,conferences,leagues " +
											       "WHERE  TeamNumber = " + teamNumber + " " +
											       "AND    leagueteams.DivisionNumber=divisions.DivisionNumber " +
											       "AND    divisions.ConferenceNumber=conferences.ConferenceNumber " +
											       "AND    conferences.LeagueNumber=leagues.LeagueNumber");
			if(queryResult.first())
			  {
			  returnInt[0]=queryResult.getInt(1);
			  returnInt[1]=queryResult.getInt(2);	
			  }
			}  
		 catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);		
			}	
		 return returnInt;
		 }       

   static void menuSpacer(PrintWriter webPageOutput)
      {
      webPageOutput.println("   <TD WIDTH=\"2\"><SPACER TYPE=\"BLOCK\" WIDTH=\"2\" HEIGHT=\"2\"></TD>");
      }

   static int doIHaveAVerifiedTeamInThisLeague(int league,
                                               Connection database,
                                               HttpSession session,
	                                           HttpServletRequest request,
	                                           ServletContext context)
      {
	  int myTeamNumber=0;
	  if("true".equals((String)session.getAttribute("password")))
		{
		myTeamNumber=Routines.getMyTeam(request,league,database,context); 
		}
      return myTeamNumber;    	
      }	

   static void dotSpacer(PrintWriter webPageOutput)
      {
      webPageOutput.println("<TABLE WIDTH=\"100%\" BORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"0\">");
      webPageOutput.println("<TBODY>");
      webPageOutput.println("<TR><TD HEIGHT=\"11\" BACKGROUND=\"../Images/dot.gif\"></TD></TR>");
      webPageOutput.println("</TBODY>");
      webPageOutput.println("</TABLE>");
      }

   static void tableStart(boolean menu,
                          PrintWriter webPageOutput)
     {
     int tableWidth=0;
     if(menu)
       {
       tableWidth=50;
       }
     else
       {
       tableWidth=100;
       }
     webPageOutput.println("<CENTER>");
     webPageOutput.println("<DIV CLASS=\"SLTables1\">");
     webPageOutput.println("<TABLE WIDTH=\"" + tableWidth + "%\" CELLPADDING=\"1\" CELLSPACING=\"1\" BORDER=\"0\">");
     webPageOutput.println("<TBODY>");
     }

   static void tableHeader(String text,
                           int colSpan,
                           PrintWriter webPageOutput)
     {
     String colSpanText="";
     if(colSpan>0)
       {
       colSpanText=" COLSPAN=\"" + colSpan + "\"";
       }
     webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
     webPageOutput.println("<TH ALIGN=\"center\"" + colSpanText + ">");
     webPageOutput.println(text);
     webPageOutput.println("</TH>");
     webPageOutput.println("</TR>");
     }

   static void tableDataStart(boolean text,
                              boolean center,
                              boolean bold,
                              boolean startLine,
                              boolean newLine,
                              int     width,
                              int     colSpan,
                              String  className,
                              PrintWriter webPageOutput)
     {
     String colSpanText="";
     if(colSpan>0)
       {
       colSpanText=" COLSPAN=\"" + colSpan + "\"";
       }
     String align="";
     if(center)
       {
       align="center";
       }
     else
       {
       if(text)
         {
         align="left";
         }
       else
         {
         align="right";
         }
       }
     if(startLine)
       {
       webPageOutput.println("<TR CLASS=\"" + className + "\">");
       }
     if(width==0)
       {
       webPageOutput.println("<TD ALIGN=\"" + align + "\"" + colSpanText + ">");
       }
     else
       {
       webPageOutput.println("<TD ALIGN=\"" + align + "\" WIDTH=\"" + width + "%\"" + colSpanText + ">");
       }
     if(bold)
       {
       webPageOutput.println("<B>");
       }
     if(newLine)
       {
       webPageOutput.println(Routines.spaceLines(1));
       }
     }

   static void tableDataEnd(boolean bold,
                            boolean newLine,
                            boolean endLine,
                            PrintWriter webPageOutput)
     {
     if(newLine)
       {
       webPageOutput.println(Routines.spaceLines(2));
       }
     if(bold)
       {
       webPageOutput.println("</B>");
       }
     webPageOutput.println("</TD>");
     if(endLine)
       {
       webPageOutput.println("</TR>");
       }
     }

   static void tableEnd(PrintWriter webPageOutput)
     {
     webPageOutput.println("</TBODY></TABLE></DIV>");
     webPageOutput.println("</CENTER>");
     }

   static void myTableStart(boolean menu,
                            PrintWriter webPageOutput)
     {
     int tableWidth=0;
     if(menu)
       {
       tableWidth=50;
       }
     else
       {
       tableWidth=100;
       }
     webPageOutput.println("<CENTER>");
     webPageOutput.println("<DIV CLASS=\"SLTables2\">");
     webPageOutput.println("<TABLE WIDTH=\"" + tableWidth + "%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
     webPageOutput.println("<TBODY>");
     }

   static void myTableHeader(String text,
                             int colSpan,
                             PrintWriter webPageOutput)
     {
     String colSpanText="";
     if(colSpan>0)
       {
       colSpanText=" COLSPAN=\"" + colSpan + "\"";
       }
     webPageOutput.println("<TR CLASS=\"bg0\" ALIGN=\"left\">");
     webPageOutput.println("<TH ALIGN=\"center\" CLASS=\"home\"" + colSpanText + ">");
     webPageOutput.println("<FONT CLASS=\"home\">");
     webPageOutput.println(text);
     webPageOutput.println("</FONT>");
     webPageOutput.println("</TH>");
     webPageOutput.println("</TR>");
     }

   static void myTableColumnHeaders(String text,
                                    PrintWriter webPageOutput)
     {
     webPageOutput.println("<TR CLASS=\"bg1\">");
     webPageOutput.println(text);
     webPageOutput.println("</TR>");
     }

   static void myTableDataStart(boolean center,
                                boolean bold,
                                boolean startLine,
                                boolean newLine,
                                int     width,
                                int     colSpan,
                                PrintWriter webPageOutput)
     {
     String colSpanText="";
     if(colSpan>0)
       {
       colSpanText=" COLSPAN=\"" + colSpan + "\"";
       }
     String align="";
     if(center)
       {
       align="center";
       }
     else
       {
       align="left";
       }
     if(startLine)
       {
       webPageOutput.println("<TR ALIGN=\"" + align + "\" CLASS=\"opt2\">");
       }
     if(width==0)
       {
       webPageOutput.println("<TD" + colSpanText + ">");
       }
     else
       {
       webPageOutput.println("<TD WIDTH=\"" + width + "%\"" + colSpanText + ">");
       }
     if(bold)
       {
       webPageOutput.println("<B>");
       }
     if(newLine)
       {
       webPageOutput.println(Routines.spaceLines(1));
       }
     }

   static String cookieCheck(HttpServletRequest request)
     {
     String userName=null;
     HttpSession session=request.getSession();
     if(session.isNew())
       {
       Cookie[] cookies = request.getCookies();
       if(cookies!=null)
         {
         for(int currentCookie=0;currentCookie<cookies.length;currentCookie++)
           {
          if(cookies[currentCookie].getName().equals("userName"))
             {
             session.setAttribute("userName",cookies[currentCookie].getValue());
             userName=cookies[currentCookie].getValue();
             }
           }
         }
       }
     return userName;
     }

   static boolean loginCheck(boolean admin,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             Connection database,
	                         ServletContext context)
      {
	  String servletName="Routines.loginCheck";	
      HttpSession session=request.getSession();
      cookieCheck(request);
//        Routines.writeToLog(servletName,"Test4:" + request.getRemoteAddr(),false,context); 
//		  Routines.writeToLog(servletName,"Test5:" + request.getRemoteUser(),false,context);
//		  Routines.writeToLog(servletName,"Test6:" + request.getRemoteHost(),false,context);     
     boolean loginRequired=false;
     String params="";
     if(admin&&!"true".equals((String)session.getAttribute("administrator")))
        {
        loginRequired=true;
        session.setAttribute("message","You don't have administrator permissions, please log in with an administrator username");
        }
     if(!loginRequired&&!"true".equals((String)session.getAttribute("password")))
        {
        loginRequired=true;
        params="userName=" + (String)session.getAttribute("userName");
        if(admin)
          {
          session.setAttribute("message","Please supply your password to access admin screens");
          }
        else
          {
          session.setAttribute("message","Please supply your password to access your private information");
          }
        }
     if(!loginRequired&&!admin)
        {
        String userName=(String)session.getAttribute("userName");
        int coachNumber=Routines.safeParseInt((String)session.getAttribute("coachNumber"));
        int teamOwner=-1;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResults = sql.executeQuery("SELECT coaches.coachNumber " +
                                                    "FROM   coaches,coachteams " +
                                                    "WHERE  UserName = '" +
                                                    userName +
                                                    "' " +
                                                    "AND TeamNumber = " +
                                                    Routines.safeParseInt(request.getParameter("team")));
          if(queryResults.first())
            {
            teamOwner=queryResults.getInt(1);
            }
          }
        catch(SQLException error)
          {
	      Routines.writeToLog(servletName,"Error getting owner of team : " + error,false,context);		
          }
        if(coachNumber!=teamOwner)
          {
          loginRequired=true;
          params="userName=" + (String)session.getAttribute("userName");
          session.setAttribute("message","This is not the correct username for the requested team, please supply the correct details");
          }
        }
      if(loginRequired)
        {
        try
          {
          int leagueNumber=Routines.safeParseInt((String)session.getAttribute("league"));
          if(leagueNumber!=0)
            {
            params=params+"&league="+leagueNumber;
            }
          response.sendRedirect(Routines.encodeURL(request,response,"SignIn",params));
          }
        catch(IOException error)
          {
          Routines.writeToLog(servletName,"No redirect possible: " + error,false,context);		
          }
        }
     return loginRequired;
     }

  static void messageCheck(boolean spacesAfter,
                           HttpServletRequest request,
                           PrintWriter webPageOutput)
     {
     HttpSession session=request.getSession();
     String message=null;
     message=(String)session.getAttribute("message");
     if(message!=null)
        {
        webPageOutput.print("<CENTER><FONT COLOR=\"#FF0000\">");
        webPageOutput.print("&nbsp*** " + message + " *** ");
        webPageOutput.println("</FONT></CENTER>");
        if(spacesAfter)
          {
          webPageOutput.println(Routines.spaceLines(2));
          }
        session.removeAttribute("message");
        }
     }

   static int safeParseInt( String str )
     {
     try
        {
        return Integer.parseInt( str );
        }
     catch( NumberFormatException e )
        {
        return 0;
        }
     catch( NullPointerException e )
       {
       return 0;
       }
     }

   static String decodeWeekNumber(int preSeasonWeeks,
                                  int regularSeasonWeeks,
                                  int postSeasonWeeks,
                                  int requiredSeason,
                                  int requiredWeek,
                                  boolean standings,
                                  HttpSession session)
      {
      String decodedText = null;
      if (requiredWeek == 0)
         {
         if(standings)
           {
           decodedText = "Start of Season";
           }
         else
           {
           decodedText = "Draft";
           }
         }
      if (requiredWeek > 0 &&
          requiredWeek <= preSeasonWeeks)
         {
         decodedText = "Preseason Week " + requiredWeek;
         }
      if (requiredWeek > preSeasonWeeks &&
          requiredWeek <= (preSeasonWeeks + regularSeasonWeeks))
         {
         decodedText = "Week " + (requiredWeek - preSeasonWeeks);
         }
      if (requiredWeek > (preSeasonWeeks + regularSeasonWeeks) &&
          postSeasonWeeks == 4)
         {
         if (requiredWeek == (preSeasonWeeks + regularSeasonWeeks + 1))
            {
            decodedText = "Wildcards";
            }
         if (requiredWeek == (preSeasonWeeks + regularSeasonWeeks + 2))
            {
            decodedText = "Divisionals";
            }
         if (requiredWeek == (preSeasonWeeks + regularSeasonWeeks + 3))
            {
            decodedText = "Conference Championships";
            }
         if (requiredWeek == (preSeasonWeeks + regularSeasonWeeks + 4))
            {
            String numerals = "";
            switch (requiredSeason)
               {
	       case 1:
                  numerals = "I";
	          break;
	       case 2:
                  numerals = "II";
	          break;
	       case 3:
                  numerals = "III";
	          break;
	       case 4:
                  numerals = "IV";
	          break;
	       case 5:
                  numerals = "V";
	          break;
	       case 6:
                  numerals = "VI";
	          break;
	       case 7:
                  numerals = "VII";
	          break;
	       case 8:
                  numerals = "VIII";
	          break;
	       case 9:
                  numerals = "IX";
	          break;
	       case 10:
                  numerals = "X";
	       case 11:
                  numerals = "XI";
	          break;
	       case 12:
                  numerals = "XII";
	          break;
	       case 13:
                  numerals = "XIII";
	          break;
	       case 14:
                  numerals = "XIV";
	          break;
	       case 15:
                  numerals = "XV";
	          break;
	       case 16:
                  numerals = "XVI";
	          break;
	       case 17:
                  numerals = "XVII";
	          break;
	       case 18:
                  numerals = "XVIII";
	          break;
	       case 19:
                  numerals = "XIX";
	          break;
	       case 20:
                  numerals = "XX";
	          break;
	       default:
                  numerals = "";
	       }
            decodedText = (String)session.getAttribute("leagueName") + " Bowl " + numerals;
            }
         }
      return decodedText;
      }

      static String getInitials(String inputString)
      {
      String[] alphabet = {"A","B","C","D","E","F","G","H","I","J","K","L","M",
                           "N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
      String returnString = new String("");
      if (inputString.substring(0,1) == " " || inputString.substring(0,1) == null)
         {
         }
      else
         {
         returnString = inputString.substring(0,1);
         }
      for (int currentDigit=1;currentDigit<(inputString.length() - 1);currentDigit++)
         {
         String previousDigit = new String(inputString.substring(currentDigit - 1,currentDigit));
         String workingDigit  = new String(inputString.substring(currentDigit,currentDigit + 1));
         if (" ".equals(previousDigit.substring(0,1)))
            {
            returnString = returnString + workingDigit;
            }
         else
            {
            for (int letter=0;letter<alphabet.length;letter++)
               {
               if (workingDigit.indexOf(alphabet[letter]) == 0)
                  {
                  returnString = returnString + workingDigit;
                  }
               }
            }
         }
      if (returnString.length() == 1)
         {
         returnString = returnString + inputString.substring(1,3);
         }
      return returnString;
      }

//	static int[] getMyScout(int            teamNumber,
//							Connection     database,
//	                        ServletContext context)
//	   {
//	   //returnInt[0] = Scout number.
//	   //returnInt[1] = Scout skills.
//	   String servletName="Routines.getMyScout";	
//	   int returnInt[] = new int[2];
//	   if(teamNumber!=0)
//	     {
//	     try
//		  {
//		  Statement sql=database.createStatement();
//		  ResultSet queryResult;
//		  queryResult=sql.executeQuery("SELECT PlayerNumber,Skill1 " +
//									   "FROM players " +
//									   "WHERE TeamNumber=" + teamNumber + " " +
//									   "AND PositionNumber=39");
//		  if(queryResult.first())
//		    {
//		    returnInt[0]=queryResult.getInt(1);
//			returnInt[1]=queryResult.getInt(2);
//			}
//		  }
//	     catch( SQLException error )
//		  {
//		  Routines.writeToLog(servletName,"Database error retrieving Scout information in method Routines.getMyScout() : " + error,false,context);	
//		  }
//	     } 
//	   return returnInt;
//	   }

   static String[] getMyTeamName(int            coachNumber,
                                 int            leagueNumber,
                                 Connection     database,
	                             ServletContext context)
      {
      //returnString[0] = Team name.
      //returnString[1] = Team number.
	  String servletName="Routines.getMyTeamName";
      String returnString[] = new String[2];
      try
         {
         Statement sql = database.createStatement();
         ResultSet rs  = sql.executeQuery("SELECT teams.Name,teams.TeamNumber " +
                                          "FROM coachteams,teams,leagueteams,divisions,conferences,leagues " +
                                          "WHERE CoachNumber = " +
                                          coachNumber + " " +
                                          "AND coachteams.TeamNumber = teams.TeamNumber " +
                                          "AND teams.TeamNumber = leagueteams.TeamNumber " +
                                          "AND leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                          "AND divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                          "AND conferences.LeagueNumber = " +
                                          leagueNumber);
         rs.first();
         returnString[0] = rs.getString(1);
         returnString[1] = String.valueOf(rs.getInt(2));
         }
      catch( SQLException error )
         {
		 Routines.writeToLog(servletName,"Database error retrieving Team information in method Routines.getMyTeamName() : " + error,false,context);		
         returnString[0] = "?????";
         }
      return returnString;
      }

   static String spaceLines(int numOfLines)
      {
      String returnString = new String("");
      for (int spacer=0;spacer<numOfLines;spacer++)
         {
         returnString = returnString + "<BR>";
         }
      return returnString;
      }

   static String indent(int numOfSpaces)
      {
      String returnString = new String("");
      for(int spacer=0;spacer<numOfSpaces;spacer++)
         {
         returnString = returnString + "&nbsp";
         }
      return returnString;
      }

   static String skillsDescription(int skillValue)
      {
      String skillDescriptions[] = {"-",
                                    "*",
                                    "**",
                                    "***",
                                    "****",
                                    "*****",
                                    "******",
                                    "*******",
                                    "********",
                                    "*********",
                                    "**********"};
      return skillDescriptions[skillValue];
      }

   static String formatName(String foreNames,
                            String surName,
                            int    fitInto)
      {
      String returnString = new String("");
      if ((foreNames.length() + surName.length() + 1) <= fitInto)
         {
         returnString = foreNames + " " + surName;
         }
      else
         {
         if ((surName.length() + 2) <= fitInto)
            {
            returnString = foreNames.substring(0,1) + " " + surName;
            }
         else
            {
            returnString = foreNames.substring(0,1) + " " + surName.substring(0,(fitInto - 4)) + "-";
            }
         }
      return returnString;
      }

   static String clubPitSig()
      {
      String returnString = new String("<BR><BR>Regards,<BR><BR><B>Club Pit</B>");
      return returnString;
      }

   static String[] getFixtureTeams(int fixtureNumber,
                                   Connection database,
	                               ServletContext context)
      {
	  String servletName="Routines.getFixtureTeams";	
      String[] returnString = {"","","","","","","",""};
      try
         {
         Statement sql = database.createStatement();
		 ResultSet queryResponse=sql.executeQuery("SELECT LeagueType " +
							                      "FROM fixtures,leagues " +
							                      "WHERE FixtureNumber=" +fixtureNumber + " " +
							                      "AND fixtures.LeagueNumber=leagues.LeagueNumber");
		 int leagueType=-1;
		 if(queryResponse.first())
		   {
		   leagueType=queryResponse.getInt(1);	                    
		   }
         queryResponse = sql.executeQuery("SELECT HomeTeam,AwayTeam " +
                                          "FROM   fixtures " +
                                          "WHERE  FixtureNumber = " + fixtureNumber);
         queryResponse.first();
         int homeTeamNumber = queryResponse.getInt(1);
         int awayTeamNumber = queryResponse.getInt(2);
         returnString[0] = String.valueOf(homeTeamNumber);
         returnString[1] = String.valueOf(awayTeamNumber);
         queryResponse = sql.executeQuery("SELECT Name,NickName,ShirtColour,NumberColour " +
                                          "FROM   teams " +
                                          "WHERE  TeamNumber = " + homeTeamNumber);
         queryResponse.first();
         if(leagueType==1)
           {
           returnString[2] = queryResponse.getString(1);
           }
	     if(leagueType==2)
		   {
		   returnString[2] = queryResponse.getString(1)+ " "+queryResponse.getString(2);
		   }            
         returnString[3] = queryResponse.getString(3);
         returnString[4] = queryResponse.getString(4);
         queryResponse = sql.executeQuery("SELECT Name,NickName,ShirtColour,NumberColour " +
                                          "FROM   teams " +
                                          "WHERE  TeamNumber = " + awayTeamNumber);
         queryResponse.first();
         returnString[5] = queryResponse.getString(1);
         returnString[6] = queryResponse.getString(3);
         returnString[7] = queryResponse.getString(4);
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving Fixture team names  : " + error,false,context);	
         }
      return returnString;
      }

   static String[] getTeamColours(int teamNumber,
                                  Connection database,
	                              ServletContext context)
      {
	  String servletName="Routines.getTeamColours";	
      String colourDetails[]={"","",""};
      int leagueInfo[]=whatLeagueAmI(teamNumber,database,context);
      try
         {
         Statement sql = database.createStatement();
         ResultSet rs  = sql.executeQuery("SELECT Name,NickName,ShirtColour,NumberColour " +
                                          "FROM   teams " +
                                          "WHERE  TeamNumber = " +
                                           teamNumber);
         rs.first();
         colourDetails[0] = rs.getString(1);
		 if(leagueInfo[1]==1)
		   {
		   colourDetails[0] = rs.getString(1);
		   }
		 if(leagueInfo[1]==2)
		   {
		   colourDetails[0] = rs.getString(1)+ " "+rs.getString(2);
		   }    
         colourDetails[1] = rs.getString(3);
         colourDetails[2] = rs.getString(4);
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving teams : " + error,false,context);		
         }
      return colourDetails;
      }

   static int getSkillRating(int numOfSkills,
                             int[] skills)
      {
      int returnInt=0;
      if(numOfSkills>0)
        {
        for(int currentSkill=0;currentSkill<numOfSkills;currentSkill++)
           {
           returnInt+=skills[currentSkill];
           }
		returnInt=returnInt/numOfSkills;   
        returnInt+=5;
        }
      return returnInt;
      }

   static String positionText(int position,
                              ServletContext context)
      {
	  String servletName="Routines.positionText";		
      String returnString="";
      int checkValue=position;
      if(position>13)
        {
        String positionString=String.valueOf(position);
        checkValue=Routines.safeParseInt(positionString.substring((positionString.length()-1),positionString.length()));
        }
      switch(checkValue)
         {
	 case 1:
            returnString=position + "st";
	    break;
         case 2:
            returnString=position + "nd";
            break;
         case 3:
            returnString=position + "rd";
            break;
         case 0:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
            returnString=position + "th";
            break;
	 default:
	        Routines.writeToLog(servletName,"No position text for Routines.positionText(): " + position,false,context);
         }
      return returnString;
      }

   static String injuryText(int length,
                            int currentSeason,
                            int currentWeek,
                            int preSeasonWeeks,
                            int regularSeasonWeeks,
                            int postSeasonWeeks,
                            int doctorVisit,
                            HttpSession session)
      {
      String returnString="";
      if(currentWeek+length>preSeasonWeeks+regularSeasonWeeks+postSeasonWeeks)
        {
        returnString = " for the season.";
        }
      else
        {
        int comeBackWeek=0;
        if(doctorVisit==0)
          {
          comeBackWeek=currentWeek+length;
          }
        else
          {
          comeBackWeek=currentWeek+length-1;
          }
        if(currentWeek+length<preSeasonWeeks+regularSeasonWeeks+postSeasonWeeks)
          {
          comeBackWeek++;
          }
        returnString = " until " + Routines.decodeWeekNumber(preSeasonWeeks,
                                                             regularSeasonWeeks,
                                                             postSeasonWeeks,
                                                             currentSeason,
                                                             comeBackWeek,
                                                             false,
                                                             session);
        }
      return returnString;
      }

   static int adjustInjuryLength(int length,
                                 int latestWeek,
                                 int currentWeek)
      {
      int returnInt=0;
      int tempAdjustment=0;
      tempAdjustment=(latestWeek + 1) - currentWeek;
      if(tempAdjustment>0)
        {
        tempAdjustment=0;
        }
      returnInt = length + tempAdjustment;
      return returnInt;
      }

   static String getRedirect(HttpServletRequest request,
                             HttpServletResponse response,
	                         ServletContext context)
      {
      String servletName="Routines.getRedirect";	
      HttpSession session=request.getSession();
      String returnString=(String)session.getAttribute("redirect");
      if("".equals(returnString))
        {
		Routines.writeToLog(servletName,"No Stored Screen!",false,context);	
        }
      returnString=response.encodeRedirectURL(returnString);
      return returnString;
      }

   static int getMyTeam(HttpServletRequest request,
                        int league,
                        Connection database,
	                    ServletContext context)
      {
	  String servletName="Routines.getMyTeam";	
      int returnInt=0;
      HttpSession session=request.getSession();
      int coach=Routines.safeParseInt((String)session.getAttribute("coachNumber"));
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResults = sql.executeQuery("SELECT leagueteams.TeamNumber " +
                                                   "FROM   coachteams,leagueteams,divisions,conferences " +
                                                   "WHERE  CoachNumber = " + coach + " " +
                                                   "AND    coachteams.TeamNumber = leagueteams.TeamNumber " +
                                                   "AND    leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                                   "AND    divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                                   "AND    conferences.LeagueNumber = " + league);
         if(queryResults.first())
           {
           returnInt=queryResults.getInt(1);
           }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error checking if coach owns teams in this league : " + error.getMessage(),false,context);		
         }
       return returnInt;
       }

//   static void cookieLogin(HttpServletRequest request,
//                           HttpServletResponse response,
//                           PrintWriter webPageOutput,
//                           Connection database,
//                           HttpSession session)
//      {
//      if(session.isNew())
//        {
//        Cookie[] cookies = request.getCookies();
//        String cookieUserName=null;
//        String cookiePassword=null;
//        if(cookies!=null)
//          {
//          for(int currentCookie=0;currentCookie<cookies.length;currentCookie++)
//            {
//            if(cookies[currentCookie].getName().equals("username"))
//              {
//              cookieUserName=cookies[currentCookie].getValue();
//              }
//            }
//	      Routines.writeToLog(servletName,"Test4:" + request.getRemoteAddr(),false,context);
//	      Routines.writeToLog(servletName,"Test5:" + request.getRemoteUser(),false,context);
//	      Routines.writeToLog(servletName,"Test6:" + request.getRemoteHost(),false,context);
//            }
//          }
//        }
//      }

   static int getTeam(int coachNumber,
                      int league,
                      Connection database,
	                  ServletContext context)
      {
	  String servletName="Routines.getTeam";	
      int returnInt=0;
      try
         {
         Statement sql=database.createStatement();
         ResultSet queryResults = sql.executeQuery("SELECT coachteams.TeamNumber " +
                                                   "FROM   coachteams,leagueteams,divisions,conferences " +
                                                   "WHERE  CoachNumber = " + coachNumber + " " +
                                                   "AND    coachteams.TeamNumber = leagueteams.TeamNumber " +
                                                   "AND    leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                                   "AND    divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                                   "AND    conferences.LeagueNumber = " + league);
         if(queryResults.first())
           {
           returnInt=queryResults.getInt(1);
           }
         }
      catch(SQLException error)
         {
	     Routines.writeToLog(servletName,"Setting team owned by coach for current league : " + error.getMessage(),false,context);			
         }
      return returnInt;
      }

   static boolean containsText(String text)
      {
      boolean returnBool=false;
      if(text==null||"".equals(text))
        {
        return returnBool;
        }
      final String[] alphabet={"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
      final String firstLetter=text.substring(0,1);
      for(int currentLetter=0;currentLetter<alphabet.length;currentLetter++)
         {
         if(firstLetter.equalsIgnoreCase(alphabet[currentLetter]))
           {
           returnBool=true;
           return returnBool;
           }
         }
      return returnBool;
      }

   static String reformatDate(Date date)
      {
      String dateString=date.toString();
      String returnString=dateString.substring(8,10) +
                          "/" +
                          dateString.substring(5,7) +
                          "/" +
                          dateString.substring(0,4);
      return returnString;
      }

   static String minorVersionText(int minorVersion)
      {
      String minorVersionText="";
      if(minorVersion>99)
        {
        minorVersionText="" + minorVersion;
        }
      else
        {
        if(minorVersion>9)
          {
          minorVersionText="0" + minorVersion;
          }
        else
          {
          minorVersionText="00" + minorVersion;
          }
        }
      return minorVersionText;
      }

  static void skillsTitleLine(String position,
                              int currentPosition,
                              boolean form,
                              PrintWriter webPageOutput)
      {
      String skills[] = {"S-M Pass","Deep Pass","Leadership","Avoid Ints",
                         "Running","Breakout","Blocking","Receiving",
                         "Pass Block","Run Block","S-M Pass","Deep Pass",
                         "Short","Medium","Deep","Agility",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Inside","Outside",
                         "Pass","Run","Tackling","Strength",
                         "Pass","Run","Tackling","Strength",
                         "Pass","Run","Tackling","Strength",
                         "Blitz","Tackling","Run","Cover",
                         "Blitz","Tackling","Run","Cover",
                         "Blitz","Tackling","Run","Cover",
                         "S-M Cover","Deep Cover","Zone Cover","Man Cover",
                         "S-M Cover","Deep Cover","Zone Cover","Man Cover",
                         "S-M Cover","Deep Cover","Zone Cover","Man Cover",
                         "<31","31-40","41-50","50+"};
      webPageOutput.println("<TR CLASS=\"bg1\">");
      webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">Sel</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"LEFT\"><FONT CLASS=\"opt2\">Name</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Exp</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                            skills[(currentPosition * 4) + 0] +
                            "</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                            skills[(currentPosition * 4) + 1] +
                            "</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                            skills[(currentPosition * 4) + 2] +
                            "</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">" +
                            skills[(currentPosition * 4) + 3] +
                            "</FONT></TD>");
      if (position.equals("Kickers"))
         {
         webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Kickoffs</FONT></TD>");
         webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Punting</FONT></TD>");
         }
      else
         {
         webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Durability</FONT></TD>");
         webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Hands</FONT></TD>");
         webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Speed</FONT></TD>");
         }
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Inj</FONT></TD>");
      webPageOutput.println("<TD ALIGN=\"RIGHT\"><FONT CLASS=\"opt2\">Frm</FONT></TD>");
      webPageOutput.println("</TR>");
      }

   static String colourOpen(int injuryValue)
      {
      String returnString = new String("");
      if(injuryValue != 0)
        {
        returnString = "<FONT COLOR=\"#FF0000\">";
        }
      return returnString;
      }

   static String colourClose(int injuryValue)
      {
      String returnString = new String("");
      if(injuryValue != 0)
        {
        returnString = "</FONT>";
        }
      return returnString;
      }

   static void countDown(Calendar target,
                         PrintWriter webPageOutput)
      {
      Date targetDate;
      Date currentDate;
      long targetDayMilliSeconds=0;
      long currentDayMilliSeconds=0;
      long diff=0;
      long years=0;
      long days=0;
      long hours=0;
      long minutes=0;
      long seconds=0;
      Calendar today=Calendar.getInstance();
      targetDate=target.getTime();
      currentDate=today.getTime();
      targetDayMilliSeconds=targetDate.getTime();
      currentDayMilliSeconds=currentDate.getTime();
      diff=targetDayMilliSeconds-currentDayMilliSeconds;
      diff=diff/1000;
      while(diff>0)
           {
           if(diff>=31536000)
             {
             years++;
             diff=diff-31536000;
             }
           else
             {
             if(diff>=86400)
               {
               days++;
               diff=diff-86400;
               }
             else
               {
               if(diff>=3600)
                 {
                 hours++;
                 diff=diff-3600;
                 }
               else
                 {
                 if(diff>=60)
                   {
                   minutes++;
                   diff=diff-60;
                   }
                 else
                   {
                   seconds=diff;
                   diff=0;
                   }
                 }
               }
             }
           }
      webPageOutput.println("<B>");
      webPageOutput.println("<SPAN ID=\"rDays\" CLASS=\"\" STYLE=\"position: relative;\"></SPAN>");
      webPageOutput.println("<SPAN ID=\"rHours\" CLASS=\"\" STYLE=\"position: relative;\"></SPAN>");
      webPageOutput.println("<SPAN ID=\"rMinutes\" CLASS=\"\" STYLE=\"position: relative;\"></SPAN>");
      webPageOutput.println("<SPAN ID=\"rSeconds\" CLASS=\"\" STYLE=\"position: relative;\"></SPAN>");
      webPageOutput.println("</B>");
      webPageOutput.println("<SCRIPT LANGUAGE=\"javascript\" TYPE=\"text/javascript\">");
      webPageOutput.println("var rDays=" + days + ";");
      webPageOutput.println("var rYears=" + years + ";");
      webPageOutput.println("var rYDays=0;");
      webPageOutput.println("var rHours=" + hours + ";");
      webPageOutput.println("var rMinutes=" + minutes + ";");
      webPageOutput.println("var rSeconds=" + seconds + ";");
      webPageOutput.println("var rMSeconds=0;");
      webPageOutput.println("function countDown()");
      webPageOutput.println("   {");
      webPageOutput.println("   if(rSeconds<0)");
      webPageOutput.println("     {rSeconds=59;rMinutes--;}");
      webPageOutput.println("   if(rMinutes<0)");
      webPageOutput.println("     {rMinutes=59;rHours--;}");
      webPageOutput.println("   if(rHours<0)");
      webPageOutput.println("     {window.location.reload()};");
      webPageOutput.println("   if(rDays!=0&&document.getElementById(\"rDays\")!= null)");
      webPageOutput.println("     {document.getElementById(\"rDays\").innerHTML = rDays == 1 ? rDays + ' day, ' : rDays + ' days, ';}");
      webPageOutput.println("   else");
      webPageOutput.println("     {");
      webPageOutput.println("     if(rYears!=0&&document.getElementById(\"rYears\")!=null)");
      webPageOutput.println("        document.getElementById(\"rYears\").innerHTML = rYears == 1 ? rYears + ' year, ' : rYears + ' years, ';");
      webPageOutput.println("     if(rYDays!=0&&document.getElementById(\"rYDays\")!=null)");
      webPageOutput.println("        document.getElementById(\"rYDays\").innerHTML = rYDays == 1 ? rYDays + ' day, ' : rYDays + ' days, ';");
      webPageOutput.println("     }");
      webPageOutput.println("   if(document.getElementById(\"rHours\")!=null)");
      webPageOutput.println("     {");
      webPageOutput.println("     if(document.getElementById(\"rMinutes\")!=null)");
      webPageOutput.println("       {");
      webPageOutput.println("       if(document.getElementById(\"rSeconds\")!=null)");
      webPageOutput.println("         {");
      webPageOutput.println("         if(rHours!=0)");
      webPageOutput.println("           document.getElementById(\"rHours\").innerHTML = rHours == 1 ? rHours + ' hour, ': rHours + ' hours, ';");
      webPageOutput.println("         if(rMinutes!=0)");
      webPageOutput.println("           document.getElementById(\"rMinutes\").innerHTML = rMinutes == 1 ? rMinutes + ' minute, ' : rMinutes + ' minutes, ';");
      webPageOutput.println("         document.getElementById(\"rSeconds\").innerHTML = rSeconds == 1 ? rSeconds + ' second' : rSeconds + ' seconds';");
      webPageOutput.println("         }");
      webPageOutput.println("       }");
      webPageOutput.println("     else");
      webPageOutput.println("       document.getElementById(\"rHours\").innerHTML = rHours == 1 ? rHours + ' hour' : rHours + ' hours' ;");
      webPageOutput.println("     }");
      webPageOutput.println("   rSeconds--;");
      webPageOutput.println("   setTimeout('countDown()',1000); }");
      webPageOutput.println("   function nonDOM()");
      webPageOutput.println("      {");
      webPageOutput.println("      if(rDays==0&&rYDays==0&&rHours==0&&rMinutes==0)");
      webPageOutput.println("        window.location.reload();");
      webPageOutput.println("      else");
      webPageOutput.println("        {");
      webPageOutput.println("        if(document.rDays!=null)");
      webPageOutput.println("          document.write(rDays+' days, ');");
      webPageOutput.println("        else");
      webPageOutput.println("          if(document.rYears!=null)");
      webPageOutput.println("            document.write(rYears + ' years, ' + rYDays + ' days, ');");
      webPageOutput.println("          if(document.rHours!=null)");
      webPageOutput.println("            document.write(rHours + ' hours, ');");
      webPageOutput.println("          if(document.rMinutes!=null)");
      webPageOutput.println("            document.write(rMinutes + ' min');");
      webPageOutput.println("        }");
      webPageOutput.println("      }");
      webPageOutput.println("document.getElementById ? countDown() : nonDOM();");
      webPageOutput.println("</SCRIPT>");
      webPageOutput.println("<NOSCRIPT>");
      webPageOutput.println(days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
      webPageOutput.println("</NOSCRIPT>");
      }

   static void scheduler(int leagueNumber,
                         Connection database,
	                     ServletContext context)
      {
	  String servletName="Routines.scheduler";	
      int leagueType=0;
      int season=0;
      int week=0;
      int scheduleInterConference=0;
      int scheduleIntraConference=0;
      int fixtureNumber=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResponse;
        queryResponse=sql.executeQuery("SELECT LeagueType,ScheduleInterConference,ScheduleIntraConference,Season,Week " +
                                       "FROM leagues " +
                                       "WHERE LeagueNumber = " + leagueNumber);
        queryResponse.first();
        leagueType=queryResponse.getInt(1);
        scheduleInterConference=queryResponse.getInt(2);
        scheduleIntraConference=queryResponse.getInt(3);
        season=queryResponse.getInt(4);
        week=queryResponse.getInt(5);
        if(week!=0)
          {
		  Routines.writeToLog(servletName,"Invalid Scheduler attempt (Week=" + week + ")",false,context);	
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);	
        }
      //Get fixture number.
      try
        {
        Statement sql = database.createStatement();
        ResultSet queryResponse;
        queryResponse = sql.executeQuery("SELECT FixtureNumber " +
                                         "FROM fixtures " +
                                         "ORDER BY FixtureNumber DESC");
        if(queryResponse.first())
          {
          fixtureNumber=queryResponse.getInt(1);
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Database error retrieving FixtureNumber : " + error,false,context);	
        }
      if(leagueType==1)
        {
        //NFL League	
        //Schedule pre-season games.	
        int[][][] fixtures=new int[20][16][2];
        int[][] teams=new int[2][16];
        int[][] divisions=new int[2][4];
        int[][] preSeasonInterDivisions=new int[2][4];
        int[][] regularSeasonInterDivisions=new int[2][4];
        int[][] intraDivisions=new int[4][2];
        int[][][] equalDivisions=new int[2][4][2];
        int currentConference=0;
        int currentDivision=0;
        int currentTeam=0;
        int topConference=0;
        int championDivision=0;
        int runnerupDivision=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResponse;
          queryResponse=sql.executeQuery("SELECT DivisionNumber,conferences.ConferenceNumber " +
                                         "FROM divisions,conferences " +
                                         "WHERE LeagueNumber = " + leagueNumber + " " +
                                         "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
                                         "ORDER BY conferences.ConferenceNumber ASC,DivisionNumber ASC");
          int tempConferenceNumber=0;
          while(queryResponse.next())
               {
               int divisionNumber=queryResponse.getInt(1);
               int conferenceNumber=queryResponse.getInt(2);
               if(tempConferenceNumber==0)
                 {
                 tempConferenceNumber=conferenceNumber;
                 }
               if(tempConferenceNumber!=conferenceNumber)
                 {
                 tempConferenceNumber=conferenceNumber;
                 currentConference++;
                 currentDivision=0;
                 }
               divisions[currentConference][currentDivision]=divisionNumber;
               currentDivision++;
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Database error retrieving divisions : " + error,false,context);	
          }
        if(scheduleInterConference==1)
          {
          regularSeasonInterDivisions[0][0]=divisions[0][0];
          regularSeasonInterDivisions[0][1]=divisions[0][1];
          regularSeasonInterDivisions[0][2]=divisions[0][2];
          regularSeasonInterDivisions[0][3]=divisions[0][3];
          regularSeasonInterDivisions[1][0]=divisions[1][0];
          regularSeasonInterDivisions[1][1]=divisions[1][1];
          regularSeasonInterDivisions[1][2]=divisions[1][2];
          regularSeasonInterDivisions[1][3]=divisions[1][3];
          preSeasonInterDivisions[0][0]=divisions[0][0];
          preSeasonInterDivisions[0][1]=divisions[0][1];
          preSeasonInterDivisions[0][2]=divisions[0][2];
          preSeasonInterDivisions[0][3]=divisions[0][3];
          preSeasonInterDivisions[1][0]=divisions[1][1];
          preSeasonInterDivisions[1][1]=divisions[1][2];
          preSeasonInterDivisions[1][2]=divisions[1][3];
          preSeasonInterDivisions[1][3]=divisions[1][0];
          }
        if(scheduleInterConference==2)
          {
          regularSeasonInterDivisions[0][0]=divisions[0][0];
          regularSeasonInterDivisions[0][1]=divisions[0][1];
          regularSeasonInterDivisions[0][2]=divisions[0][2];
          regularSeasonInterDivisions[0][3]=divisions[0][3];
          regularSeasonInterDivisions[1][0]=divisions[1][1];
          regularSeasonInterDivisions[1][1]=divisions[1][2];
          regularSeasonInterDivisions[1][2]=divisions[1][3];
          regularSeasonInterDivisions[1][3]=divisions[1][0];
          preSeasonInterDivisions[0][0]=divisions[0][0];
          preSeasonInterDivisions[0][1]=divisions[0][1];
          preSeasonInterDivisions[0][2]=divisions[0][2];
          preSeasonInterDivisions[0][3]=divisions[0][3];
          preSeasonInterDivisions[1][0]=divisions[1][2];
          preSeasonInterDivisions[1][1]=divisions[1][3];
          preSeasonInterDivisions[1][2]=divisions[1][0];
          preSeasonInterDivisions[1][3]=divisions[1][1];
          }
        if(scheduleInterConference==3)
          {
          regularSeasonInterDivisions[0][0]=divisions[0][0];
          regularSeasonInterDivisions[0][1]=divisions[0][1];
          regularSeasonInterDivisions[0][2]=divisions[0][2];
          regularSeasonInterDivisions[0][3]=divisions[0][3];
          regularSeasonInterDivisions[1][0]=divisions[1][2];
          regularSeasonInterDivisions[1][1]=divisions[1][3];
          regularSeasonInterDivisions[1][2]=divisions[1][0];
          regularSeasonInterDivisions[1][3]=divisions[1][1];
          preSeasonInterDivisions[0][0]=divisions[0][0];
          preSeasonInterDivisions[0][1]=divisions[0][1];
          preSeasonInterDivisions[0][2]=divisions[0][2];
          preSeasonInterDivisions[0][3]=divisions[0][3];
          preSeasonInterDivisions[1][0]=divisions[1][3];
          preSeasonInterDivisions[1][1]=divisions[1][0];
          preSeasonInterDivisions[1][2]=divisions[1][1];
          preSeasonInterDivisions[1][3]=divisions[1][2];
          }
        if(scheduleInterConference==4)
          {
          regularSeasonInterDivisions[0][0]=divisions[0][0];
          regularSeasonInterDivisions[0][1]=divisions[0][1];
          regularSeasonInterDivisions[0][2]=divisions[0][2];
          regularSeasonInterDivisions[0][3]=divisions[0][3];
          regularSeasonInterDivisions[1][0]=divisions[1][3];
          regularSeasonInterDivisions[1][1]=divisions[1][0];
          regularSeasonInterDivisions[1][2]=divisions[1][1];
          regularSeasonInterDivisions[1][3]=divisions[1][2];
          preSeasonInterDivisions[0][0]=divisions[0][0];
          preSeasonInterDivisions[0][1]=divisions[0][1];
          preSeasonInterDivisions[0][2]=divisions[0][2];
          preSeasonInterDivisions[0][3]=divisions[0][3];
          preSeasonInterDivisions[1][0]=divisions[1][0];
          preSeasonInterDivisions[1][1]=divisions[1][1];
          preSeasonInterDivisions[1][2]=divisions[1][2];
          preSeasonInterDivisions[1][3]=divisions[1][3];
          }
        if(scheduleIntraConference==1)
          {
          intraDivisions[0][0]=divisions[0][0];
          intraDivisions[0][1]=divisions[0][1];
          intraDivisions[1][0]=divisions[0][2];
          intraDivisions[1][1]=divisions[0][3];
          intraDivisions[2][0]=divisions[1][0];
          intraDivisions[2][1]=divisions[1][1];
          intraDivisions[3][0]=divisions[1][2];
          intraDivisions[3][1]=divisions[1][3];
          equalDivisions[0][0][0]=divisions[0][0];
          equalDivisions[0][0][1]=divisions[0][2];
          equalDivisions[0][1][0]=divisions[0][1];
          equalDivisions[0][1][1]=divisions[0][3];
          equalDivisions[0][2][0]=divisions[1][0];
          equalDivisions[0][2][1]=divisions[1][2];
          equalDivisions[0][3][0]=divisions[1][1];
          equalDivisions[0][3][1]=divisions[1][3];
          equalDivisions[1][0][0]=divisions[0][3];
          equalDivisions[1][0][1]=divisions[0][0];
          equalDivisions[1][1][0]=divisions[0][2];
          equalDivisions[1][1][1]=divisions[0][1];
          equalDivisions[1][2][0]=divisions[1][3];
          equalDivisions[1][2][1]=divisions[1][0];
          equalDivisions[1][3][0]=divisions[1][2];
          equalDivisions[1][3][1]=divisions[1][1];
          }
        if(scheduleIntraConference==2)
          {
          intraDivisions[0][0]=divisions[0][1];
          intraDivisions[0][1]=divisions[0][2];
          intraDivisions[1][0]=divisions[0][3];
          intraDivisions[1][1]=divisions[0][0];
          intraDivisions[2][0]=divisions[1][1];
          intraDivisions[2][1]=divisions[1][2];
          intraDivisions[3][0]=divisions[1][3];
          intraDivisions[3][1]=divisions[1][0];
          equalDivisions[0][0][0]=divisions[0][1];
          equalDivisions[0][0][1]=divisions[0][0];
          equalDivisions[0][1][0]=divisions[0][2];
          equalDivisions[0][1][1]=divisions[0][3];
          equalDivisions[0][2][0]=divisions[1][1];
          equalDivisions[0][2][1]=divisions[1][0];
          equalDivisions[0][3][0]=divisions[1][3];
          equalDivisions[0][3][1]=divisions[1][2];
          equalDivisions[1][0][0]=divisions[0][1];
          equalDivisions[1][0][1]=divisions[0][0];
          equalDivisions[1][1][0]=divisions[0][2];
          equalDivisions[1][1][1]=divisions[0][3];
          equalDivisions[1][2][0]=divisions[1][1];
          equalDivisions[1][2][1]=divisions[1][0];
          equalDivisions[1][3][0]=divisions[1][3];
          equalDivisions[1][3][1]=divisions[1][2];
          }
        if(scheduleIntraConference==3)
          {
          intraDivisions[0][0]=divisions[0][0];
          intraDivisions[0][1]=divisions[0][2];
          intraDivisions[1][0]=divisions[0][1];
          intraDivisions[1][1]=divisions[0][3];
          intraDivisions[2][0]=divisions[1][0];
          intraDivisions[2][1]=divisions[1][2];
          intraDivisions[3][0]=divisions[1][1];
          intraDivisions[3][1]=divisions[1][3];
          equalDivisions[0][0][0]=divisions[0][1];
          equalDivisions[0][0][1]=divisions[0][0];
          equalDivisions[0][1][0]=divisions[0][3];
          equalDivisions[0][1][1]=divisions[0][2];
          equalDivisions[0][2][0]=divisions[1][0];
          equalDivisions[0][2][1]=divisions[1][1];
          equalDivisions[0][3][0]=divisions[1][3];
          equalDivisions[0][3][1]=divisions[1][2];
          equalDivisions[1][0][0]=divisions[0][0];
          equalDivisions[1][0][1]=divisions[0][3];
          equalDivisions[1][1][0]=divisions[0][2];
          equalDivisions[1][1][1]=divisions[0][1];
          equalDivisions[1][2][0]=divisions[1][0];
          equalDivisions[1][2][1]=divisions[1][3];
          equalDivisions[1][3][0]=divisions[1][2];
          equalDivisions[1][3][1]=divisions[1][1];
          }
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResponse;
          queryResponse=sql.executeQuery("SELECT TeamNumber,conferences.ConferenceNumber,Rank,divisions.DivisionNumber " +
                                         "FROM leagueteams,divisions,conferences " +
                                         "WHERE LeagueNumber = " + leagueNumber + " " +
                                         "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
                                         "AND divisions.DivisionNumber=leagueteams.divisionNumber " +
                                         "ORDER BY conferences.ConferenceNumber ASC,Rank ASC");
          int tempConferenceNumber=0;
          currentConference=0;
          currentTeam=0;
          while(queryResponse.next())
               {
               int teamNumber=queryResponse.getInt(1);
               int conferenceNumber=queryResponse.getInt(2);
               int rank=queryResponse.getInt(3);
               int divisionNumber=queryResponse.getInt(4);
               if(tempConferenceNumber==0)
                 {
                 tempConferenceNumber=conferenceNumber;
                 championDivision=divisionNumber;
                 }
               if(tempConferenceNumber!=conferenceNumber)
                 {
                 runnerupDivision=divisionNumber;
                 tempConferenceNumber=conferenceNumber;
                 currentConference++;
                 currentTeam=0;
                 }
               teams[currentConference][currentTeam]=teamNumber;
               if(rank==1)
                 {
                 topConference=currentConference;
                 }
               currentTeam++;
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Database error retrieving teams : " + error,false,context);	
          }
        boolean superBowlRematch=false;
        for(int currentMatchup=0;currentMatchup<4;currentMatchup++)
           {
           if((regularSeasonInterDivisions[0][currentMatchup]==championDivision&&regularSeasonInterDivisions[1][currentMatchup]==runnerupDivision)||
              (regularSeasonInterDivisions[1][currentMatchup]==championDivision&&regularSeasonInterDivisions[0][currentMatchup]==runnerupDivision))
              {
              superBowlRematch=true;
              }
           }
        if(!superBowlRematch)
          {
          int championConference=0;
          int championDivisionArray=0;
          int runnerupDivisionArray=0;
          for(currentDivision=0;currentDivision<4;currentDivision++)
             {
             if(divisions[0][currentDivision]==championDivision)
               {
               championDivisionArray=divisions[0][currentDivision];
               }
             if(divisions[1][currentDivision]==championDivision)
               {
               championConference=1;
               championDivisionArray=divisions[1][currentDivision];
               }
             if(divisions[0][currentDivision]==runnerupDivision)
               {
               runnerupDivisionArray=divisions[0][currentDivision];
               }
             if(divisions[1][currentDivision]==championDivision)
               {
               runnerupDivisionArray=divisions[1][currentDivision];
               }
             }
          if(championConference==0)
            {
            if(championDivisionArray==0&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              }
            }
          else
            {
            if(championDivisionArray==0&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==0)
              {
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              preSeasonInterDivisions[0][0]=divisions[0][0];
              preSeasonInterDivisions[0][1]=divisions[0][1];
              preSeasonInterDivisions[0][2]=divisions[0][2];
              preSeasonInterDivisions[0][3]=divisions[0][3];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              preSeasonInterDivisions[0][0]=divisions[0][1];
              preSeasonInterDivisions[0][1]=divisions[0][2];
              preSeasonInterDivisions[0][2]=divisions[0][3];
              preSeasonInterDivisions[0][3]=divisions[0][0];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==1)
              {
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==2)
              {
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              preSeasonInterDivisions[0][0]=divisions[0][2];
              preSeasonInterDivisions[0][1]=divisions[0][3];
              preSeasonInterDivisions[0][2]=divisions[0][0];
              preSeasonInterDivisions[0][3]=divisions[0][1];
              }
            if(championDivisionArray==0&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[1][0]=divisions[1][0];
              preSeasonInterDivisions[1][1]=divisions[1][1];
              preSeasonInterDivisions[1][2]=divisions[1][2];
              preSeasonInterDivisions[1][3]=divisions[1][3];
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              }
            if(championDivisionArray==1&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[1][0]=divisions[1][1];
              preSeasonInterDivisions[1][1]=divisions[1][2];
              preSeasonInterDivisions[1][2]=divisions[1][3];
              preSeasonInterDivisions[1][3]=divisions[1][0];
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              }
            if(championDivisionArray==2&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[1][0]=divisions[1][2];
              preSeasonInterDivisions[1][1]=divisions[1][3];
              preSeasonInterDivisions[1][2]=divisions[1][0];
              preSeasonInterDivisions[1][3]=divisions[1][1];
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              }
            if(championDivisionArray==3&&runnerupDivisionArray==3)
              {
              preSeasonInterDivisions[1][0]=divisions[1][3];
              preSeasonInterDivisions[1][1]=divisions[1][0];
              preSeasonInterDivisions[1][2]=divisions[1][1];
              preSeasonInterDivisions[1][3]=divisions[1][2];
              preSeasonInterDivisions[0][0]=divisions[0][3];
              preSeasonInterDivisions[0][1]=divisions[0][0];
              preSeasonInterDivisions[0][2]=divisions[0][1];
              preSeasonInterDivisions[0][3]=divisions[0][2];
              }
            }
          }
        int homeConference=0;
        int awayConference=0;
        if(topConference==0)
          {
          homeConference=1;
          }
        else
          {
          awayConference=1;
          }
        //Set Pre Season Fixtures
        for(int currentWeek=0;currentWeek<4;currentWeek++)
           {
           for(int currentMatchUp=0;currentMatchUp<4;currentMatchUp++)
              {
              int preSeasonTeams[][]=new int[2][4];
              for(int awayHome=0;awayHome<2;awayHome++)
                 {
                 try
                   {
                   Statement sql=database.createStatement();
                   ResultSet queryResponse;
                   queryResponse=sql.executeQuery("SELECT TeamNumber " +
                                                  "FROM leagueteams " +
                                                  "WHERE DivisionNumber = " + preSeasonInterDivisions[awayHome][currentMatchUp] + " " +
                                                  "ORDER BY Rank ASC");
                   currentTeam=0;
                   while(queryResponse.next())
                        {
                        preSeasonTeams[awayHome][currentTeam]=queryResponse.getInt(1);
                        currentTeam++;
                        }
                   }
                 catch(SQLException error)
                      {
					  Routines.writeToLog(servletName,"Database error retrieving preseason teams : " + error,false,context);	
                      }
                 }
              if(currentWeek==0)
                {
                fixtures[currentWeek][(currentMatchUp*4)+0][0]=preSeasonTeams[0][0];
                fixtures[currentWeek][(currentMatchUp*4)+0][1]=preSeasonTeams[1][0];
                fixtures[currentWeek][(currentMatchUp*4)+1][0]=preSeasonTeams[0][1];
                fixtures[currentWeek][(currentMatchUp*4)+1][1]=preSeasonTeams[1][1];
                fixtures[currentWeek][(currentMatchUp*4)+2][0]=preSeasonTeams[0][2];
                fixtures[currentWeek][(currentMatchUp*4)+2][1]=preSeasonTeams[1][2];
                fixtures[currentWeek][(currentMatchUp*4)+3][0]=preSeasonTeams[0][3];
                fixtures[currentWeek][(currentMatchUp*4)+3][1]=preSeasonTeams[1][3];
                }
              if(currentWeek==1)
                {
                fixtures[currentWeek][(currentMatchUp*4)+0][1]=preSeasonTeams[0][0];
                fixtures[currentWeek][(currentMatchUp*4)+0][0]=preSeasonTeams[1][1];
                fixtures[currentWeek][(currentMatchUp*4)+1][1]=preSeasonTeams[0][1];
                fixtures[currentWeek][(currentMatchUp*4)+1][0]=preSeasonTeams[1][2];
                fixtures[currentWeek][(currentMatchUp*4)+2][1]=preSeasonTeams[0][2];
                fixtures[currentWeek][(currentMatchUp*4)+2][0]=preSeasonTeams[1][3];
                fixtures[currentWeek][(currentMatchUp*4)+3][1]=preSeasonTeams[0][3];
                fixtures[currentWeek][(currentMatchUp*4)+3][0]=preSeasonTeams[1][0];
                }
              if(currentWeek==2)
                {
                fixtures[currentWeek][(currentMatchUp*4)+0][0]=preSeasonTeams[0][0];
                fixtures[currentWeek][(currentMatchUp*4)+0][1]=preSeasonTeams[1][2];
                fixtures[currentWeek][(currentMatchUp*4)+1][0]=preSeasonTeams[0][1];
                fixtures[currentWeek][(currentMatchUp*4)+1][1]=preSeasonTeams[1][3];
                fixtures[currentWeek][(currentMatchUp*4)+2][0]=preSeasonTeams[0][2];
                fixtures[currentWeek][(currentMatchUp*4)+2][1]=preSeasonTeams[1][0];
                fixtures[currentWeek][(currentMatchUp*4)+3][0]=preSeasonTeams[0][3];
                fixtures[currentWeek][(currentMatchUp*4)+3][1]=preSeasonTeams[1][1];
                }
              if(currentWeek==3)
                {
                fixtures[currentWeek][(currentMatchUp*4)+0][1]=preSeasonTeams[0][0];
                fixtures[currentWeek][(currentMatchUp*4)+0][0]=preSeasonTeams[1][3];
                fixtures[currentWeek][(currentMatchUp*4)+1][1]=preSeasonTeams[0][1];
                fixtures[currentWeek][(currentMatchUp*4)+1][0]=preSeasonTeams[1][0];
                fixtures[currentWeek][(currentMatchUp*4)+2][1]=preSeasonTeams[0][2];
                fixtures[currentWeek][(currentMatchUp*4)+2][0]=preSeasonTeams[1][1];
                fixtures[currentWeek][(currentMatchUp*4)+3][1]=preSeasonTeams[0][3];
                fixtures[currentWeek][(currentMatchUp*4)+3][0]=preSeasonTeams[1][2];
                }
             }
           }
        //Set Inter Conference Fixtures
        for(int currentWeek=0;currentWeek<4;currentWeek++)
           {
           for(int currentMatchUp=0;currentMatchUp<4;currentMatchUp++)
              {
              int regularSeasonTeams[][]=new int[2][4];
              for(int awayHome=0;awayHome<2;awayHome++)
                 {
                 try
                   {
                   Statement sql=database.createStatement();
                   ResultSet queryResponse;
                   queryResponse=sql.executeQuery("SELECT TeamNumber " +
                                                  "FROM leagueteams " +
                                                  "WHERE DivisionNumber = " + regularSeasonInterDivisions[awayHome][currentMatchUp] + " " +
                                                  "ORDER BY Rank ASC");
                   currentTeam=0;
                   while(queryResponse.next())
                        {
                        regularSeasonTeams[awayHome][currentTeam]=queryResponse.getInt(1);
                        currentTeam++;
                        }
                   }
                 catch(SQLException error)
                      {
					  Routines.writeToLog(servletName,"Database error retrieving preseason teams : " + error,false,context);	
                      }
                 }
              if(currentWeek==0)
                {
                fixtures[4][(currentMatchUp*4)+0][0]=regularSeasonTeams[0][0];
                fixtures[4][(currentMatchUp*4)+0][1]=regularSeasonTeams[1][0];
                fixtures[4][(currentMatchUp*4)+1][0]=regularSeasonTeams[0][1];
                fixtures[4][(currentMatchUp*4)+1][1]=regularSeasonTeams[1][1];
                fixtures[4][(currentMatchUp*4)+2][0]=regularSeasonTeams[0][2];
                fixtures[4][(currentMatchUp*4)+2][1]=regularSeasonTeams[1][2];
                fixtures[4][(currentMatchUp*4)+3][0]=regularSeasonTeams[0][3];
                fixtures[4][(currentMatchUp*4)+3][1]=regularSeasonTeams[1][3];
                }
              if(currentWeek==1)
                {
                fixtures[5][(currentMatchUp*4)+0][1]=regularSeasonTeams[0][0];
                fixtures[5][(currentMatchUp*4)+0][0]=regularSeasonTeams[1][1];
                fixtures[5][(currentMatchUp*4)+1][1]=regularSeasonTeams[0][1];
                fixtures[5][(currentMatchUp*4)+1][0]=regularSeasonTeams[1][0];
                fixtures[5][(currentMatchUp*4)+2][1]=regularSeasonTeams[0][2];
                fixtures[5][(currentMatchUp*4)+2][0]=regularSeasonTeams[1][3];
                fixtures[5][(currentMatchUp*4)+3][1]=regularSeasonTeams[0][3];
                fixtures[5][(currentMatchUp*4)+3][0]=regularSeasonTeams[1][2];
                }
              if(currentWeek==2)
                {
                fixtures[11][(currentMatchUp*4)+0][0]=regularSeasonTeams[0][0];
                fixtures[11][(currentMatchUp*4)+0][1]=regularSeasonTeams[1][2];
                fixtures[11][(currentMatchUp*4)+1][0]=regularSeasonTeams[0][1];
                fixtures[11][(currentMatchUp*4)+1][1]=regularSeasonTeams[1][3];
                fixtures[11][(currentMatchUp*4)+2][0]=regularSeasonTeams[0][2];
                fixtures[11][(currentMatchUp*4)+2][1]=regularSeasonTeams[1][0];
                fixtures[11][(currentMatchUp*4)+3][0]=regularSeasonTeams[0][3];
                fixtures[11][(currentMatchUp*4)+3][1]=regularSeasonTeams[1][1];
                }
              if(currentWeek==3)
                {
                fixtures[12][(currentMatchUp*4)+0][1]=regularSeasonTeams[0][0];
                fixtures[12][(currentMatchUp*4)+0][0]=regularSeasonTeams[1][3];
                fixtures[12][(currentMatchUp*4)+1][1]=regularSeasonTeams[0][1];
                fixtures[12][(currentMatchUp*4)+1][0]=regularSeasonTeams[1][2];
                fixtures[12][(currentMatchUp*4)+2][1]=regularSeasonTeams[0][2];
                fixtures[12][(currentMatchUp*4)+2][0]=regularSeasonTeams[1][1];
                fixtures[12][(currentMatchUp*4)+3][1]=regularSeasonTeams[0][3];
                fixtures[12][(currentMatchUp*4)+3][0]=regularSeasonTeams[1][0];
                }
             }
           }
        //Set Intra Conference Fixtures
        for(int currentWeek=0;currentWeek<4;currentWeek++)
           {
           for(int currentMatchUp=0;currentMatchUp<4;currentMatchUp++)
              {
              int regularSeasonTeams[][]=new int[2][4];
              for(int awayHome=0;awayHome<2;awayHome++)
                 {
                 try
                   {
                   Statement sql=database.createStatement();
                   ResultSet queryResponse;
                   queryResponse=sql.executeQuery("SELECT TeamNumber " +
                                                  "FROM leagueteams " +
                                                  "WHERE DivisionNumber = " + intraDivisions[currentMatchUp][awayHome] + " " +
                                                  "ORDER BY Rank ASC");
                   currentTeam=0;
                   while(queryResponse.next())
                        {
                        regularSeasonTeams[awayHome][currentTeam]=queryResponse.getInt(1);
                        currentTeam++;
                        }
                   }
                 catch(SQLException error)
                      {
					  Routines.writeToLog(servletName,"Database error retrieving preseason teams : " + error,false,context);	
                      }
                 }
              if(currentWeek==0)
                {
                fixtures[7][(currentMatchUp*4)+0][0]=regularSeasonTeams[0][0];
                fixtures[7][(currentMatchUp*4)+0][1]=regularSeasonTeams[1][3];
                fixtures[7][(currentMatchUp*4)+1][0]=regularSeasonTeams[0][1];
                fixtures[7][(currentMatchUp*4)+1][1]=regularSeasonTeams[1][2];
                fixtures[7][(currentMatchUp*4)+2][0]=regularSeasonTeams[0][2];
                fixtures[7][(currentMatchUp*4)+2][1]=regularSeasonTeams[1][1];
                fixtures[7][(currentMatchUp*4)+3][0]=regularSeasonTeams[0][3];
                fixtures[7][(currentMatchUp*4)+3][1]=regularSeasonTeams[1][0];
                }
              if(currentWeek==1)
                {
                fixtures[9][(currentMatchUp*4)+0][1]=regularSeasonTeams[0][0];
                fixtures[9][(currentMatchUp*4)+0][0]=regularSeasonTeams[1][2];
                fixtures[9][(currentMatchUp*4)+1][1]=regularSeasonTeams[0][1];
                fixtures[9][(currentMatchUp*4)+1][0]=regularSeasonTeams[1][3];
                fixtures[9][(currentMatchUp*4)+2][1]=regularSeasonTeams[0][2];
                fixtures[9][(currentMatchUp*4)+2][0]=regularSeasonTeams[1][0];
                fixtures[9][(currentMatchUp*4)+3][1]=regularSeasonTeams[0][3];
                fixtures[9][(currentMatchUp*4)+3][0]=regularSeasonTeams[1][1];
                }
              if(currentWeek==2)
                {
                fixtures[13][(currentMatchUp*4)+0][0]=regularSeasonTeams[0][0];
                fixtures[13][(currentMatchUp*4)+0][1]=regularSeasonTeams[1][1];
                fixtures[13][(currentMatchUp*4)+1][0]=regularSeasonTeams[0][1];
                fixtures[13][(currentMatchUp*4)+1][1]=regularSeasonTeams[1][0];
                fixtures[13][(currentMatchUp*4)+2][0]=regularSeasonTeams[0][2];
                fixtures[13][(currentMatchUp*4)+2][1]=regularSeasonTeams[1][3];
                fixtures[13][(currentMatchUp*4)+3][0]=regularSeasonTeams[0][3];
                fixtures[13][(currentMatchUp*4)+3][1]=regularSeasonTeams[1][2];
                }
              if(currentWeek==3)
                {
                fixtures[14][(currentMatchUp*4)+0][1]=regularSeasonTeams[0][0];
                fixtures[14][(currentMatchUp*4)+0][0]=regularSeasonTeams[1][0];
                fixtures[14][(currentMatchUp*4)+1][1]=regularSeasonTeams[0][1];
                fixtures[14][(currentMatchUp*4)+1][0]=regularSeasonTeams[1][1];
                fixtures[14][(currentMatchUp*4)+2][1]=regularSeasonTeams[0][2];
                fixtures[14][(currentMatchUp*4)+2][0]=regularSeasonTeams[1][2];
                fixtures[14][(currentMatchUp*4)+3][1]=regularSeasonTeams[0][3];
                fixtures[14][(currentMatchUp*4)+3][0]=regularSeasonTeams[1][3];
                }
             }
           }
        //Set Divisional Fixtures
        for(int currentWeek=0;currentWeek<6;currentWeek++)
           {
           for(currentConference=0;currentConference<2;currentConference++)
              {
              for(currentDivision=0;currentDivision<4;currentDivision++)
                 {
                 int divisionTeams[]=new int[4];
                 try
                   {
                   Statement sql=database.createStatement();
                   ResultSet queryResponse;
                   queryResponse=sql.executeQuery("SELECT TeamNumber " +
                                                  "FROM leagueteams " +
                                                  "WHERE DivisionNumber = " + divisions[currentConference][currentDivision] + " " +
                                                  "ORDER BY Rank ASC");
                   currentTeam=0;
                   while(queryResponse.next())
                        {
                        divisionTeams[currentTeam]=queryResponse.getInt(1);
                        currentTeam++;
                        }
                   }
                 catch(SQLException error)
                      {
					  Routines.writeToLog(servletName,"Database error retrieving division teams : " + error,false,context);	
                      }
                 int conferenceAdjustment=0;
                 if(currentConference==1)
                   {
                   conferenceAdjustment=8;
                   }
                 if(currentWeek==0)
                   {
                   fixtures[6][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
                   fixtures[6][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[1];
                   fixtures[6][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[2];
                   fixtures[6][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[3];
                   }
                 if(currentWeek==1)
                   {
                   fixtures[8][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[2];
                   fixtures[8][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
                   fixtures[8][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[3];
                   fixtures[8][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[1];
                   }
                 if(currentWeek==2)
                   {
                   fixtures[10][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
                   fixtures[10][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[3];
                   fixtures[10][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[1];
                   fixtures[10][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[2];
                   }
                 if(currentWeek==3)
                   {
                   fixtures[16][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[3];
                   fixtures[16][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
                   fixtures[16][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[2];
                   fixtures[16][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[1];
                   }
                 if(currentWeek==4)
                   {
                   fixtures[17][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
                   fixtures[17][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[2];
                   fixtures[17][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[1];
                   fixtures[17][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[3];
                   }
                 if(currentWeek==5)
                   {
                   fixtures[19][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[1];
                   fixtures[19][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
                   fixtures[19][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[3];
                   fixtures[19][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[2];
                   }
                }
             }
           }
        //Set Equal Team Fixtures
        for(int currentWeek=0;currentWeek<2;currentWeek++)
           {
           for(int currentMatchUp=0;currentMatchUp<4;currentMatchUp++)
              {
              int equalTeams[][]=new int[4][2];
              for(currentDivision=0;currentDivision<2;currentDivision++)
                 {
                 try
                   {
                   Statement sql=database.createStatement();
                   ResultSet queryResponse;
                   queryResponse=sql.executeQuery("SELECT TeamNumber " +
                                                  "FROM leagueteams " +
                                                  "WHERE DivisionNumber = " + equalDivisions[currentWeek][currentMatchUp][currentDivision] + " " +
                                                  "ORDER BY Rank ASC");
                   currentTeam=0;
                   while(queryResponse.next())
                        {
                        equalTeams[currentTeam][currentDivision]=queryResponse.getInt(1);
                        currentTeam++;
                        }
                   }
                 catch(SQLException error)
                      {
					  Routines.writeToLog(servletName,"Database error retrieving equal teams : " + error,false,context);	
                      }
                 if(currentWeek==0)
                   {
                   fixtures[15][(currentMatchUp*4)+0][0]=equalTeams[0][0];
                   fixtures[15][(currentMatchUp*4)+0][1]=equalTeams[0][1];
                   fixtures[15][(currentMatchUp*4)+1][0]=equalTeams[1][0];
                   fixtures[15][(currentMatchUp*4)+1][1]=equalTeams[1][1];
                   fixtures[15][(currentMatchUp*4)+2][0]=equalTeams[2][0];
                   fixtures[15][(currentMatchUp*4)+2][1]=equalTeams[2][1];
                   fixtures[15][(currentMatchUp*4)+3][0]=equalTeams[3][0];
                   fixtures[15][(currentMatchUp*4)+3][1]=equalTeams[3][1];
                   }
                 if(currentWeek==1)
                   {
                   fixtures[18][(currentMatchUp*4)+0][0]=equalTeams[0][0];
                   fixtures[18][(currentMatchUp*4)+0][1]=equalTeams[0][1];
                   fixtures[18][(currentMatchUp*4)+1][0]=equalTeams[1][0];
                   fixtures[18][(currentMatchUp*4)+1][1]=equalTeams[1][1];
                   fixtures[18][(currentMatchUp*4)+2][0]=equalTeams[2][0];
                   fixtures[18][(currentMatchUp*4)+2][1]=equalTeams[2][1];
                   fixtures[18][(currentMatchUp*4)+3][0]=equalTeams[3][0];
                   fixtures[18][(currentMatchUp*4)+3][1]=equalTeams[3][1];
                   }
                }
             }
           }
        for(int currentWeek=0;currentWeek<20;currentWeek++)
           {
           for(int currentFixture=0;currentFixture<16;currentFixture++)
              {
              try
                {
                Statement sql=database.createStatement();
                int updated=0;
                fixtureNumber++;
                updated=sql.executeUpdate("INSERT INTO fixtures " +
                                          "(FixtureNumber,LeagueNumber,Season,Week,HomeTeam,AwayTeam,Game,DateTimeStamp)" +
                                          " VALUES (" +
                                          fixtureNumber + "," +
                                          leagueNumber + "," +
                                          (season+1) + "," +
                                          (currentWeek+1) + "," +
                                          fixtures[currentWeek][currentFixture][1] + "," +
                                          fixtures[currentWeek][currentFixture][0] + "," +
                                          currentFixture + ",'" +
                                          Routines.getDateTime(false) + "')");
                if(updated==0)
                  {
				  Routines.writeToLog(servletName,"Fixture not created",false,context);
                  }
                }
              catch(SQLException error)
                   {
				   Routines.writeToLog(servletName,"Database error updating fixtures : " + error,false,context);
                   }
              }
           }
        }
		if(leagueType==2)
		  {
		  //UNFL League	
		  //Schedule pre-season games.	
		  int[][][] fixtures=new int[20][10][2];
		  int[][] teams=new int[2][10];
		  int[][] divisions=new int[2][2];
		  int[][] preSeasonInterDivisions=new int[2][2];
		  int[][] regularSeasonInterDivisions=new int[4][2];
		  int[][][] equalDivisions=new int[2][2][2];
		  int currentConference=0;
		  int currentDivision=0;
		  int currentTeam=0;
		  int topConference=0;
		  int championDivision=0;
		  int runnerupDivision=0;
		  try
			{
			Statement sql=database.createStatement();
			ResultSet queryResponse;
			queryResponse=sql.executeQuery("SELECT DivisionNumber,conferences.ConferenceNumber " +
										   "FROM divisions,conferences " +
										   "WHERE LeagueNumber = " + leagueNumber + " " +
										   "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
										   "ORDER BY conferences.ConferenceNumber ASC,DivisionNumber ASC");
			int tempConferenceNumber=0;
			while(queryResponse.next())
				 {
				 int divisionNumber=queryResponse.getInt(1);
				 int conferenceNumber=queryResponse.getInt(2);
				 if(tempConferenceNumber==0)
				   {
				   tempConferenceNumber=conferenceNumber;
				   }
				 if(tempConferenceNumber!=conferenceNumber)
				   {
				   tempConferenceNumber=conferenceNumber;
				   currentConference++;
				   currentDivision=0;
				   }
				 divisions[currentConference][currentDivision]=divisionNumber;
				 currentDivision++;
				 }
			}
		  catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Database error retrieving divisions : " + error,false,context);	
			}
		  if(scheduleInterConference==1)
			{
			preSeasonInterDivisions[0][0]=divisions[0][0];
			preSeasonInterDivisions[0][1]=divisions[0][1];
			preSeasonInterDivisions[1][0]=divisions[1][0];
			preSeasonInterDivisions[1][1]=divisions[1][1];
			regularSeasonInterDivisions[0][0]=divisions[0][0];
			regularSeasonInterDivisions[0][1]=divisions[1][1];
			regularSeasonInterDivisions[1][0]=divisions[0][1];
			regularSeasonInterDivisions[1][1]=divisions[1][0];
			regularSeasonInterDivisions[2][0]=divisions[1][0];
			regularSeasonInterDivisions[2][1]=divisions[0][0];
			regularSeasonInterDivisions[3][0]=divisions[1][1];
			regularSeasonInterDivisions[3][1]=divisions[0][1];			
			}
		  if(scheduleInterConference==2)
			{
			preSeasonInterDivisions[0][0]=divisions[0][1];
			preSeasonInterDivisions[0][1]=divisions[0][0];
			preSeasonInterDivisions[1][0]=divisions[1][1];
			preSeasonInterDivisions[1][1]=divisions[1][0];
			regularSeasonInterDivisions[0][0]=divisions[1][1];
			regularSeasonInterDivisions[0][1]=divisions[0][1];
			regularSeasonInterDivisions[1][0]=divisions[1][0];
			regularSeasonInterDivisions[1][1]=divisions[0][0];
			regularSeasonInterDivisions[2][0]=divisions[0][0];
			regularSeasonInterDivisions[2][1]=divisions[1][1];
			regularSeasonInterDivisions[3][0]=divisions[0][1];
			regularSeasonInterDivisions[3][1]=divisions[1][0];	
			}
		  try
			{
			Statement sql=database.createStatement();
			ResultSet queryResponse;
			queryResponse=sql.executeQuery("SELECT TeamNumber,conferences.ConferenceNumber,Rank,divisions.DivisionNumber " +
										   "FROM leagueteams,divisions,conferences " +
										   "WHERE LeagueNumber = " + leagueNumber + " " +
										   "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
										   "AND divisions.DivisionNumber=leagueteams.divisionNumber " +
										   "ORDER BY conferences.ConferenceNumber ASC,Rank ASC");
			int tempConferenceNumber=0;
			currentConference=0;
			currentTeam=0;
			while(queryResponse.next())
				 {
				 int teamNumber=queryResponse.getInt(1);
				 int conferenceNumber=queryResponse.getInt(2);
				 int rank=queryResponse.getInt(3);
				 int divisionNumber=queryResponse.getInt(4);
				 if(tempConferenceNumber==0)
				   {
				   tempConferenceNumber=conferenceNumber;
				   championDivision=divisionNumber;
				   }
				 if(tempConferenceNumber!=conferenceNumber)
				   {
				   runnerupDivision=divisionNumber;
				   tempConferenceNumber=conferenceNumber;
				   currentConference++;
				   currentTeam=0;
				   }
				 teams[currentConference][currentTeam]=teamNumber;
				 if(rank==1)
				   {
				   topConference=currentConference;
				   }
				 currentTeam++;
				 }
			}
		  catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Database error retrieving teams : " + error,false,context);	
			}
		  int homeConference=0;
		  int awayConference=0;
		  if(topConference==0)
			{
			homeConference=1;
			}
		  else
			{
			awayConference=1;
			}
		  //Set Pre Season Fixtures
		  for(int currentWeek=0;currentWeek<4;currentWeek++)
			 {
			 for(int currentMatchUp=0;currentMatchUp<2;currentMatchUp++)
				{
				int preSeasonTeams[][]=new int[2][5];
				for(int awayHome=0;awayHome<2;awayHome++)
				   {
				   try
					 {
					 Statement sql=database.createStatement();
					 ResultSet queryResponse;
					 queryResponse=sql.executeQuery("SELECT TeamNumber " +
													"FROM leagueteams " +
													"WHERE DivisionNumber = " + preSeasonInterDivisions[awayHome][currentMatchUp] + " " +
													"ORDER BY Rank ASC");
					 currentTeam=0;
					 while(queryResponse.next())
						  {
						  preSeasonTeams[awayHome][currentTeam]=queryResponse.getInt(1);
						  currentTeam++;
						  }
					 }
				   catch(SQLException error)
						{
						Routines.writeToLog(servletName,"Database error retrieving preseason teams : " + error,false,context);	
						}
				   }
				if(currentWeek==0)
				  {
				  fixtures[currentWeek][(currentMatchUp*5)+0][0]=preSeasonTeams[0][0];
				  fixtures[currentWeek][(currentMatchUp*5)+0][1]=preSeasonTeams[1][2];
				  fixtures[currentWeek][(currentMatchUp*5)+1][0]=preSeasonTeams[0][1];
				  fixtures[currentWeek][(currentMatchUp*5)+1][1]=preSeasonTeams[1][4];
				  fixtures[currentWeek][(currentMatchUp*5)+2][0]=preSeasonTeams[0][2];
				  fixtures[currentWeek][(currentMatchUp*5)+2][1]=preSeasonTeams[1][1];
				  fixtures[currentWeek][(currentMatchUp*5)+3][0]=preSeasonTeams[0][3];
				  fixtures[currentWeek][(currentMatchUp*5)+3][1]=preSeasonTeams[1][0];
				  fixtures[currentWeek][(currentMatchUp*5)+4][0]=preSeasonTeams[0][4];
				  fixtures[currentWeek][(currentMatchUp*5)+4][1]=preSeasonTeams[1][3];
				  }
				if(currentWeek==1)
				  {
				  fixtures[currentWeek][(currentMatchUp*5)+0][1]=preSeasonTeams[0][0];
				  fixtures[currentWeek][(currentMatchUp*5)+0][0]=preSeasonTeams[1][1];
				  fixtures[currentWeek][(currentMatchUp*5)+1][1]=preSeasonTeams[0][1];
				  fixtures[currentWeek][(currentMatchUp*5)+1][0]=preSeasonTeams[1][2];
				  fixtures[currentWeek][(currentMatchUp*5)+2][1]=preSeasonTeams[0][2];
				  fixtures[currentWeek][(currentMatchUp*5)+2][0]=preSeasonTeams[1][3];
				  fixtures[currentWeek][(currentMatchUp*5)+3][1]=preSeasonTeams[0][3];
				  fixtures[currentWeek][(currentMatchUp*5)+3][0]=preSeasonTeams[1][4];
				  fixtures[currentWeek][(currentMatchUp*5)+4][1]=preSeasonTeams[0][4];
				  fixtures[currentWeek][(currentMatchUp*5)+4][0]=preSeasonTeams[1][0];
				  }
				if(currentWeek==2)
				  {
				  fixtures[currentWeek][(currentMatchUp*5)+0][0]=preSeasonTeams[0][4];
				  fixtures[currentWeek][(currentMatchUp*5)+0][1]=preSeasonTeams[1][2];
				  fixtures[currentWeek][(currentMatchUp*5)+1][0]=preSeasonTeams[0][1];
				  fixtures[currentWeek][(currentMatchUp*5)+1][1]=preSeasonTeams[1][3];
				  fixtures[currentWeek][(currentMatchUp*5)+2][0]=preSeasonTeams[0][2];
				  fixtures[currentWeek][(currentMatchUp*5)+2][1]=preSeasonTeams[1][0];
				  fixtures[currentWeek][(currentMatchUp*5)+3][0]=preSeasonTeams[0][3];
				  fixtures[currentWeek][(currentMatchUp*5)+3][1]=preSeasonTeams[1][1];
				  fixtures[currentWeek][(currentMatchUp*5)+4][0]=preSeasonTeams[0][0];
				  fixtures[currentWeek][(currentMatchUp*5)+4][1]=preSeasonTeams[1][4];
				  }
				if(currentWeek==3)
				  {
				  fixtures[currentWeek][(currentMatchUp*5)+0][1]=preSeasonTeams[0][0];
				  fixtures[currentWeek][(currentMatchUp*5)+0][0]=preSeasonTeams[1][3];
				  fixtures[currentWeek][(currentMatchUp*5)+1][1]=preSeasonTeams[0][1];
				  fixtures[currentWeek][(currentMatchUp*5)+1][0]=preSeasonTeams[1][0];
				  fixtures[currentWeek][(currentMatchUp*5)+2][1]=preSeasonTeams[0][2];
				  fixtures[currentWeek][(currentMatchUp*5)+2][0]=preSeasonTeams[1][4];
				  fixtures[currentWeek][(currentMatchUp*5)+3][1]=preSeasonTeams[0][3];
				  fixtures[currentWeek][(currentMatchUp*5)+3][0]=preSeasonTeams[1][2];
				  fixtures[currentWeek][(currentMatchUp*5)+4][1]=preSeasonTeams[0][4];
				  fixtures[currentWeek][(currentMatchUp*5)+4][0]=preSeasonTeams[1][1];
				  }
			   }
			 }
		  //Set Inter Conference Fixtures
		  for(int currentWeek=0;currentWeek<2;currentWeek++)
			 {
			 int regularSeasonTeams[][]=new int[2][5];	
			 for(currentConference=0;currentConference<2;currentConference++)
				{
  			    for(currentDivision=0;currentDivision<2;currentDivision++)
			       {	
			       try
					 {
					 Statement sql=database.createStatement();
					 ResultSet queryResponse;
					 queryResponse=sql.executeQuery("SELECT TeamNumber " +
													"FROM leagueteams " +
													"WHERE DivisionNumber = " + regularSeasonInterDivisions[(currentWeek*2)+currentConference][currentDivision] + " " +
													"ORDER BY Rank ASC");
					 currentTeam=0;
					 while(queryResponse.next())
						{
						regularSeasonTeams[currentDivision][currentTeam]=queryResponse.getInt(1);
						currentTeam++;
						}
					 }
				   catch(SQLException error)
					 {
					 Routines.writeToLog(servletName,"Database error retrieving inter division UNFL teams : " + error,false,context);	
					 }
				   }
				if(currentWeek==0)
				  {   
				  fixtures[5][currentConference+8][0]=regularSeasonTeams[0][0];
				  fixtures[5][currentConference+8][1]=regularSeasonTeams[1][0];
				  fixtures[6][currentConference+8][0]=regularSeasonTeams[0][1];
				  fixtures[6][currentConference+8][1]=regularSeasonTeams[1][1];
				  fixtures[9][currentConference+8][0]=regularSeasonTeams[0][2];
				  fixtures[9][currentConference+8][1]=regularSeasonTeams[1][2];
				  fixtures[10][currentConference+8][0]=regularSeasonTeams[0][3];
				  fixtures[10][currentConference+8][1]=regularSeasonTeams[1][3];
				  fixtures[12][currentConference+8][0]=regularSeasonTeams[0][4];
				  fixtures[12][currentConference+8][1]=regularSeasonTeams[1][4];
				  }
				if(currentWeek==1)
				  {   
				  fixtures[14][currentConference+8][0]=regularSeasonTeams[0][0];
				  fixtures[14][currentConference+8][1]=regularSeasonTeams[1][0];
				  fixtures[15][currentConference+8][0]=regularSeasonTeams[0][1];
				  fixtures[15][currentConference+8][1]=regularSeasonTeams[1][1];
				  fixtures[16][currentConference+8][0]=regularSeasonTeams[0][2];
				  fixtures[16][currentConference+8][1]=regularSeasonTeams[1][2];
				  fixtures[18][currentConference+8][0]=regularSeasonTeams[0][3];
				  fixtures[18][currentConference+8][1]=regularSeasonTeams[1][3];
				  fixtures[19][currentConference+8][0]=regularSeasonTeams[0][4];
				  fixtures[19][currentConference+8][1]=regularSeasonTeams[1][4];
				  }
				}
			 }
		  //Set Intra Conference Fixtures
		  for(int currentWeek=0;currentWeek<6;currentWeek++)
			 {
			 for(int currentMatchUp=0;currentMatchUp<2;currentMatchUp++)
				{
				int regularSeasonTeams[][]=new int[2][5];
				for(int awayHome=0;awayHome<2;awayHome++)
				   {
				   try
					 {
					 Statement sql=database.createStatement();
					 ResultSet queryResponse;
					 queryResponse=sql.executeQuery("SELECT TeamNumber " +
													"FROM leagueteams " +
													"WHERE DivisionNumber = " + divisions[currentMatchUp][awayHome] + " " +
													"ORDER BY Rank ASC");
					 currentTeam=0;
					 while(queryResponse.next())
						  {
						  regularSeasonTeams[awayHome][currentTeam]=queryResponse.getInt(1);
						  currentTeam++;
						  }
					 }
				   catch(SQLException error)
						{
						Routines.writeToLog(servletName,"Database error retrieving UNFL intra division teams : " + error,false,context);	
						}
				   }
				if(currentWeek==0)
				  {
				  fixtures[4][(currentMatchUp*5)+0][0]=regularSeasonTeams[0][0];
				  fixtures[4][(currentMatchUp*5)+0][1]=regularSeasonTeams[1][0];
				  fixtures[4][(currentMatchUp*5)+1][0]=regularSeasonTeams[0][1];
				  fixtures[4][(currentMatchUp*5)+1][1]=regularSeasonTeams[1][1];
				  fixtures[4][(currentMatchUp*5)+2][0]=regularSeasonTeams[0][2];
				  fixtures[4][(currentMatchUp*5)+2][1]=regularSeasonTeams[1][2];
				  fixtures[4][(currentMatchUp*5)+3][0]=regularSeasonTeams[0][3];
				  fixtures[4][(currentMatchUp*5)+3][1]=regularSeasonTeams[1][3];
				  fixtures[4][(currentMatchUp*5)+4][0]=regularSeasonTeams[0][4];
				  fixtures[4][(currentMatchUp*5)+4][1]=regularSeasonTeams[1][4];
				  }		
				if(currentWeek==1)
				  {
				  fixtures[7][(currentMatchUp*5)+0][1]=regularSeasonTeams[0][0];
				  fixtures[7][(currentMatchUp*5)+0][0]=regularSeasonTeams[1][1];
                  fixtures[7][(currentMatchUp*5)+1][1]=regularSeasonTeams[0][1];
				  fixtures[7][(currentMatchUp*5)+1][0]=regularSeasonTeams[1][2];
				  fixtures[7][(currentMatchUp*5)+2][1]=regularSeasonTeams[0][2];
				  fixtures[7][(currentMatchUp*5)+2][0]=regularSeasonTeams[1][3];
				  fixtures[7][(currentMatchUp*5)+3][1]=regularSeasonTeams[0][3];
				  fixtures[7][(currentMatchUp*5)+3][0]=regularSeasonTeams[1][4];
				  fixtures[7][(currentMatchUp*5)+4][1]=regularSeasonTeams[0][4];
				  fixtures[7][(currentMatchUp*5)+4][0]=regularSeasonTeams[1][0];
				  }
				if(currentWeek==2)
				  {
				  fixtures[8][(currentMatchUp*5)+0][0]=regularSeasonTeams[0][0];
				  fixtures[8][(currentMatchUp*5)+0][1]=regularSeasonTeams[1][2];
				  fixtures[8][(currentMatchUp*5)+1][0]=regularSeasonTeams[0][1];
				  fixtures[8][(currentMatchUp*5)+1][1]=regularSeasonTeams[1][3];
				  fixtures[8][(currentMatchUp*5)+2][0]=regularSeasonTeams[0][2];
				  fixtures[8][(currentMatchUp*5)+2][1]=regularSeasonTeams[1][4];
				  fixtures[8][(currentMatchUp*5)+3][0]=regularSeasonTeams[0][3];
				  fixtures[8][(currentMatchUp*5)+3][1]=regularSeasonTeams[1][0];
				  fixtures[8][(currentMatchUp*5)+4][0]=regularSeasonTeams[0][4];
				  fixtures[8][(currentMatchUp*5)+4][1]=regularSeasonTeams[1][1];
				  }
				if(currentWeek==3)
				  {
				  fixtures[11][(currentMatchUp*5)+0][1]=regularSeasonTeams[0][0];
				  fixtures[11][(currentMatchUp*5)+0][0]=regularSeasonTeams[1][3];
				  fixtures[11][(currentMatchUp*5)+1][1]=regularSeasonTeams[0][1];
				  fixtures[11][(currentMatchUp*5)+1][0]=regularSeasonTeams[1][4];
				  fixtures[11][(currentMatchUp*5)+2][1]=regularSeasonTeams[0][2];
				  fixtures[11][(currentMatchUp*5)+2][0]=regularSeasonTeams[1][0];
				  fixtures[11][(currentMatchUp*5)+3][1]=regularSeasonTeams[0][3];
				  fixtures[11][(currentMatchUp*5)+3][0]=regularSeasonTeams[1][1];
				  fixtures[11][(currentMatchUp*5)+4][1]=regularSeasonTeams[0][4];
				  fixtures[11][(currentMatchUp*5)+4][0]=regularSeasonTeams[1][2];
				  }
				if(currentWeek==4)
				  {
				  fixtures[13][(currentMatchUp*5)+0][0]=regularSeasonTeams[0][0];
				  fixtures[13][(currentMatchUp*5)+0][1]=regularSeasonTeams[1][4];
				  fixtures[13][(currentMatchUp*5)+1][0]=regularSeasonTeams[0][1];
				  fixtures[13][(currentMatchUp*5)+1][1]=regularSeasonTeams[1][0];
				  fixtures[13][(currentMatchUp*5)+2][0]=regularSeasonTeams[0][2];
				  fixtures[13][(currentMatchUp*5)+2][1]=regularSeasonTeams[1][1];
				  fixtures[13][(currentMatchUp*5)+3][0]=regularSeasonTeams[0][3];
				  fixtures[13][(currentMatchUp*5)+3][1]=regularSeasonTeams[1][2];
				  fixtures[13][(currentMatchUp*5)+4][0]=regularSeasonTeams[0][4];
				  fixtures[13][(currentMatchUp*5)+4][1]=regularSeasonTeams[1][3];
				  }				  
			   if(currentWeek==5)
				 {
				 fixtures[17][(currentMatchUp*5)+0][1]=regularSeasonTeams[0][0];
				 fixtures[17][(currentMatchUp*5)+0][0]=regularSeasonTeams[1][0];
				 fixtures[17][(currentMatchUp*5)+1][1]=regularSeasonTeams[0][1];
				 fixtures[17][(currentMatchUp*5)+1][0]=regularSeasonTeams[1][1];
				 fixtures[17][(currentMatchUp*5)+2][1]=regularSeasonTeams[0][2];
				 fixtures[17][(currentMatchUp*5)+2][0]=regularSeasonTeams[1][2];
				 fixtures[17][(currentMatchUp*5)+3][1]=regularSeasonTeams[0][3];
				 fixtures[17][(currentMatchUp*5)+3][0]=regularSeasonTeams[1][3];
				 fixtures[17][(currentMatchUp*5)+4][1]=regularSeasonTeams[0][4];
				 fixtures[17][(currentMatchUp*5)+4][0]=regularSeasonTeams[1][4];
				 }
		       }
			 }
		  //Set Divisional Fixtures
		  for(int currentWeek=0;currentWeek<10;currentWeek++)
			 {
			 for(currentConference=0;currentConference<2;currentConference++)
				{
				for(currentDivision=0;currentDivision<2;currentDivision++)
				   {
				   int divisionTeams[]=new int[5];
				   try
					 {
					 Statement sql=database.createStatement();
					 ResultSet queryResponse;
					 queryResponse=sql.executeQuery("SELECT TeamNumber " +
													"FROM leagueteams " +
													"WHERE DivisionNumber = " + divisions[currentConference][currentDivision] + " " +
													"ORDER BY Rank ASC");
					 currentTeam=0;
					 while(queryResponse.next())
						  {
						  divisionTeams[currentTeam]=queryResponse.getInt(1);
						  currentTeam++;
						  }
					 }
				   catch(SQLException error)
						{
						Routines.writeToLog(servletName,"Database error retrieving division teams : " + error,false,context);	
						}
				   int conferenceAdjustment=0;
				   if(currentConference==1)
					 {
					 conferenceAdjustment=4;
					 }
				   if(currentWeek==0)
					 {
					 fixtures[5][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[1];
					 fixtures[5][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[2];
					 fixtures[5][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[3];
					 fixtures[5][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[4];
					 }
				   if(currentWeek==1)
					 {
					 fixtures[6][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[3];
					 fixtures[6][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
					 fixtures[6][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[4];
					 fixtures[6][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[2];
					 }
				   if(currentWeek==2)
					 {
					 fixtures[9][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
					 fixtures[9][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[4];
					 fixtures[9][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[1];
					 fixtures[9][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[3];
					 }
				   if(currentWeek==3)
					 {
					 fixtures[10][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[4];
					 fixtures[10][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[1];
					 fixtures[10][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[2];
					 fixtures[10][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[0];
					 }
				   if(currentWeek==4)
					 {
					 fixtures[12][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
					 fixtures[12][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[1];
					 fixtures[12][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[2];
					 fixtures[12][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[3];
					 }
				  if(currentWeek==5)
				    {
					fixtures[14][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[1];
					fixtures[14][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[2];
					fixtures[14][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[3];
					fixtures[14][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[4];
					}
				  if(currentWeek==6)
					{
					fixtures[15][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[3];
					fixtures[15][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[0];
					fixtures[15][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[4];
					fixtures[15][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[2];
					}
				  if(currentWeek==7)
					{
					fixtures[16][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
					fixtures[16][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[4];
					fixtures[16][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[1];
					fixtures[16][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[3];
					}
				  if(currentWeek==8)
					{
					fixtures[18][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[4];
					fixtures[18][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[1];
					fixtures[18][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[2];
					fixtures[18][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[0];
					}
				  if(currentWeek==9)
					{
					fixtures[19][(currentDivision*2)+0+conferenceAdjustment][1]=divisionTeams[0];
					fixtures[19][(currentDivision*2)+0+conferenceAdjustment][0]=divisionTeams[1];
					fixtures[19][(currentDivision*2)+1+conferenceAdjustment][1]=divisionTeams[2];
					fixtures[19][(currentDivision*2)+1+conferenceAdjustment][0]=divisionTeams[3];
					}
				  }
			   }
			 }
		  int numOfFixturesPerWeek=0;
		  if(leagueType==1)
		    { 
		    numOfFixturesPerWeek=16;	
		    }
		  if(leagueType==2)
		    {
		    numOfFixturesPerWeek=10;	  
		    }
		  for(int currentWeek=0;currentWeek<20;currentWeek++)
			 {
			 for(int currentFixture=0;currentFixture<numOfFixturesPerWeek;currentFixture++)
				{
				try
				  {
				  Statement sql=database.createStatement();
				  int updated=0;
				  fixtureNumber++;
				  updated=sql.executeUpdate("INSERT INTO fixtures " +
											"(FixtureNumber,LeagueNumber,Season,Week,HomeTeam,AwayTeam,Game,DateTimeStamp)" +
											" VALUES (" +
											fixtureNumber + "," +
											leagueNumber + "," +
											(season+1) + "," +
											(currentWeek+1) + "," +
											fixtures[currentWeek][currentFixture][1] + "," +
											fixtures[currentWeek][currentFixture][0] + "," +
											currentFixture + ",'" +
											Routines.getDateTime(false) + "')");
				  if(updated==0)
					{
					Routines.writeToLog(servletName,"Fixture not created",false,context);
					}
				  }
				catch(SQLException error)
					 {
					 Routines.writeToLog(servletName,"Database error updating fixtures : " + error,false,context);
					 }
				}
			 }
		  }       
      //Schedule created, increment schedule counters.
      if(leagueType==1)
        {
        //NFL	
        if(scheduleInterConference<4)
          {
          scheduleInterConference++;
          }
        else
          {
          scheduleInterConference=1;
          }
        if(scheduleIntraConference<3)
          {
          scheduleIntraConference++;
          }
        else
          {
          scheduleIntraConference=1;
          }
        }
	   if(leagueType==2)
		 {
		 //UNFL	
		 if(scheduleInterConference<2)
		   {
		   scheduleInterConference++;
		   }
		 else
		   {
		   scheduleInterConference=1;
		   }
		 }        
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResponse;
        int updated=0;
        season++;
        updated=sql.executeUpdate("UPDATE leagues " +
                                  "SET Season=" + season + "," +
                                  "ScheduleInterConference=" + scheduleInterConference + "," +
                                  "ScheduleIntraConference=" + scheduleIntraConference + "," +
                                  "DateTimeStamp='" + Routines.getDateTime(true) + "' " +
                                  "WHERE LeagueNumber = " + leagueNumber);
        if(updated==0)
          {
		  Routines.writeToLog(servletName,"League table not updated (LeagueNumber=" +
                                         leagueNumber +
                                         ")",false,context);
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);
        }
      }

   static int[] getNumOfNames(ServletContext context)
      {
	  String servletName="Routines.getNumOfNames";	
      int[] returnInt=new int[2];
      try
        {
        String file=context.getRealPath("/");
        FileReader forNames=new FileReader(file + "/Data/fornames.dat");
        BufferedReader forNamesBuffer=new BufferedReader(forNames);
        boolean endOfFile=false;
        while(!endOfFile)
             {
             String forName=forNamesBuffer.readLine();
             if(forName==null)
               {
               endOfFile=true;
               }
             else
               {
               returnInt[0]++;
               }
             }
       forNamesBuffer.close();
        }
      catch(IOException error)
        {
		Routines.writeToLog(servletName,"Problem with surnames file : " + error,false,context);
        }
      try
        {
        String file=context.getRealPath("/");
        FileReader surNames=new FileReader(file + "/Data/surnames.dat");
        BufferedReader surNamesBuffer=new BufferedReader(surNames);
        boolean endOfFile=false;
        while(!endOfFile)
             {
             String surName=surNamesBuffer.readLine();
             if(surName==null)
               {
               endOfFile=true;
               }
             else
               {
               returnInt[1]++;
               }
             }
       surNamesBuffer.close();
        }
      catch(IOException error)
        {
		Routines.writeToLog(servletName,"Problem with surnames file : " + error,false,context);
        }
      return returnInt;
      }

   static boolean writeToLog(String className,
                             String text,
                             boolean newFile,
                             ServletContext context)
      {
	  String servletName="Routines.writeToLog";	
      FileWriter logFile;
      BufferedWriter logFileBuffer;
      synchronized(logLock)
        {
        String date=Routines.getDateTime(false);
        String file=context.getRealPath("/");
        if(newFile)
          {
          try
            {
            logFile=new FileWriter(file + "/Data/log.txt");
            logFileBuffer=new BufferedWriter(logFile);
            logFileBuffer.write(date + " : (" + className + ") " + text);
            logFileBuffer.newLine();
            logFileBuffer.close();
            logFile=new FileWriter(file + "/Data/log.txt",true);
            logFileBuffer=new BufferedWriter(logFile);
            }
          catch(IOException error2)
            {
			Routines.writeToLog(servletName,"Unable to create log file : " + error2,false,context);	
            return false;
            }
          }
        else
          {
          try
            {
            logFile=new FileWriter(file + "/Data/log.txt",true);
            logFileBuffer=new BufferedWriter(logFile);
            logFileBuffer.write(date + " : (" + className + ") " + text);
            logFileBuffer.newLine();
            logFileBuffer.close();
            }
          catch(IOException error)
            {
			Routines.writeToLog(servletName,"Problem with log file : " + error,false,context);	
            return false;
            }
          }
        return true;
        }
      }

	static void getWebPage(String webPage,
	            Connection database,
	            HttpSession session,
	            boolean firstTime,
	            ServletContext context)
	   {
	   String servletName="Routines.getWebPage";	
	   String todaysDate=getDateTime(true);
	   boolean errorFound=false;
	   int year=safeParseInt(todaysDate.substring(0,4));
	   if(firstTime)
	      {
	      try{
		     Statement sql=database.createStatement();
		     ResultSet queryResults = sql.executeQuery("SELECT Season " +
			   									       "FROM   masterplayers " +
												       "WHERE  Season = " +
												       year + ";");
		     if(queryResults.first())
		       {
			   session.setAttribute("message","Players already uploaded for "+year);	
			   return;
		       }
	         }
	      catch(SQLException error)
	         {
	         Routines.writeToLog(servletName,"Unable to get masterplayers : " + error,false,context);
		     session.setAttribute("message","Unable to get masterplayers : " + error);
		     errorFound=true;
		     return;
	         }   
	      }   
	   try{
		  URL url = new URL(webPage);
		  URLConnection connection = null;
		  InputStreamReader  inStream = null;
		  BufferedReader buffer = null;
		  connection = url.openConnection();
		  inStream = new InputStreamReader(connection.getInputStream());
		  buffer = new BufferedReader(inStream);
 		  boolean endOfFile=false;
		  while(!endOfFile)
			   {
			   String webPageLine=buffer.readLine();
			   if(webPageLine==null)
			     {
			     endOfFile=true;
			     }
			   else
			     {
			     if(webPageLine.indexOf("ALPHABETICAL LISTING FOR")>0)
			       {	
			       int currentPos=webPageLine.indexOf("/players/playerpage/");
			       int currentPlayer=0;
			       while(currentPos>0)
			          {
			          currentPlayer++;
			          String playerNumber=webPageLine.substring(currentPos+20,currentPos+30);
			          int tempPos=playerNumber.indexOf("\"");
			          if(tempPos>0)
			             {
                         playerNumber=playerNumber.substring(0,tempPos);
                         playerNumber.trim();	
			             currentPos=webPageLine.indexOf("/players/playerpage/",currentPos+1);
			             }
			          try
			            {
					    URL playerUrl = new URL("http://www.nfl.com/players/playerpage/"+playerNumber);
					    URLConnection playerConnection = null;
					    InputStreamReader  playerInStream = null;
					    BufferedReader playerBuffer = null;
					    playerConnection = playerUrl.openConnection();
					    playerInStream = new InputStreamReader(playerConnection.getInputStream());
					    playerBuffer = new BufferedReader(playerInStream);
					    String college="";
						int preferredNumber=0;
						String foreName="";
						String surName="";	
						int positionNumber=0;
						int height=0;
						int weight=0;
						int experience=0;
						int dd=0;
						int mm=0;
						int ccyy=0;
						int masterPlayerNumber=0;
						int collegeNumber=0;
					    boolean playerEndOfFile=false;
					    while(!playerEndOfFile)
					 	     {
							 String playerPageLine=playerBuffer.readLine();
						     if(playerPageLine==null)
						       {
						       playerEndOfFile=true;
						       }
						     else
						       {
						       int twoDigitShirtNumber=0;
						       if(playerPageLine.indexOf("class=\"player\"")>0)
							     {	
							     int charPosition=playerPageLine.indexOf("class=\"player\"");
							     if(playerPageLine.substring(charPosition+17,charPosition+18).equals(" "))
								   {
								   }	
								 else
								   { 
							       preferredNumber=safeParseInt(playerPageLine.substring(charPosition+17,charPosition+18));
								   twoDigitShirtNumber++;
			         		       if(playerPageLine.substring(charPosition+18,charPosition+19).equals(" "))
							         {
							         }	
							       else
							         {  	
								     preferredNumber=(preferredNumber*10)+safeParseInt(playerPageLine.substring(charPosition+18,charPosition+19));
								     twoDigitShirtNumber++;	
							         }
								   } 
								 String fullName=playerPageLine.substring(charPosition+18+twoDigitShirtNumber,playerPageLine.length()-5);
							     foreName=fullName.substring(0,fullName.indexOf(" "));
							     surName=fullName.substring(fullName.indexOf(" ")+1,fullName.length());
								 System.out.println("player="+fullName);
								 }
							   if(playerPageLine.indexOf("Position: ")>-1)
								 {
								 positionNumber=0;		
								 int charPosition=playerPageLine.indexOf("Position: ");
								 String position=playerPageLine.substring(10,13);
								 if(position.equals("QB<"))
								   {
								   positionNumber=14;	
								   }
								 if(position.equals("RB<"))
								   {
								   positionNumber=15;	
								   }  	
								 if(position.equals("FB<"))
								   {
								   positionNumber=16;	
								   }
								 if(position.equals("WR<"))
								   {
								   positionNumber=13;	
								   }
								 if(position.equals("TE<"))
								   {
								   positionNumber=12;
								   }  		
								 if(position.equals("OL<"))
								   {
								   positionNumber=56;	
								   }
								 if(position.equals("C<b"))
								   {
								   positionNumber=7;	
								   }  	
								 if(position.equals("G<b"))
								   {
								   positionNumber=8;	
								   }
								 if(position.equals("T<b"))
								   {
								   positionNumber=9;	
								   }
								 if(position.equals("DE<"))
								   {
								   positionNumber=17;	
								   }
								 if(position.equals("DL<"))
								   {
								   positionNumber=57;	
								   } 								    	
								 if((position.equals("DT<"))||(position.equals("NT<")))
								   {
								   positionNumber=18;	
								   } 
								 if(position.equals("CB<"))
								   {
								   positionNumber=21;	
								   }
								 if((position.equals("ILB"))||(position.equals("MLB")))
								   {
								   positionNumber=19;	
								   } 
								 if(position.equals("OLB"))
								   {
								   positionNumber=20;	
								   } 
								 if(position.equals("LB<"))
								   {
								   positionNumber=54;	
								   } 
								 if(position.equals("FS<"))
								   {
								   positionNumber=26;	
								   } 
								 if(position.equals("DB<"))
								   {
								   positionNumber=53;	
								   } 								    
								 if(position.equals("SS<"))
								   {
								   positionNumber=27;	
								   }  	
								 if(position.equals("K<b"))
								   {
								   positionNumber=1;	
								   }
								 if(position.equals("P<b"))
								   {
								   positionNumber=2;	
								   }  								   										   							   
								 if(position.equals("LS<"))
								   {
								   positionNumber=55;	
								   }  
								 if(positionNumber==0)
								   {
								   Routines.writeToLog(servletName,"Position not found : " + position,false,context);
						   	       System.out.println("PositionNotFound="+position);
								   session.setAttribute("message","Position not found : " + position);
								   errorFound=true;
								   return;
	                               }	  
								 }
							   if(playerPageLine.indexOf("Height: ")>-1)
								 {	
								 int charPosition=playerPageLine.indexOf("Height: ");
								 int heightFeet=safeParseInt(playerPageLine.substring(8,9));
								 int heightInches=0;
								 if(playerPageLine.substring(11,12).equals("<"))
								   {
								   heightInches=safeParseInt(playerPageLine.substring(10,11));	
								   }
								 else
								   {
								   heightInches=safeParseInt(playerPageLine.substring(10,12));	
								   }	
								 height=(heightFeet*12)+heightInches;
								 }										 							     
							   if(playerPageLine.indexOf("Weight: ")>-1)
								 {	
								 int charPosition=playerPageLine.indexOf("Weight: ");
								 weight=safeParseInt(playerPageLine.substring(8,11));
								 }
							   if(playerPageLine.indexOf("Born: ")>-1)
								 {	
								 int charPosition=playerPageLine.indexOf("Born: ");
								 if(!playerPageLine.substring(6,8).equals("<b"))
								   {
								   mm=safeParseInt(playerPageLine.substring(6,8));
								   dd=safeParseInt(playerPageLine.substring(9,11));
								   ccyy=safeParseInt(playerPageLine.substring(12,16));
								   }
								 if(ccyy==00)
								   {
								   mm=01;
								   dd=01;
								   ccyy=year-22;	
								   }	  
								 }
							   if(playerPageLine.indexOf("College: ")>-1)
								 {	
								 int charPosition=playerPageLine.indexOf("College: ");
								 college=playerPageLine.substring(9,playerPageLine.length()-4);
								 }
							   if(playerPageLine.indexOf("NFL Experience: ")>-1)
								 {
								 if(playerPageLine.length()>16)
								   {		
								   int charPosition=playerPageLine.indexOf("NFL Experience: ");
								   if(playerPageLine.length()==18)
								     {
								     experience=safeParseInt(playerPageLine.substring(16,18));	
								     }
								   else
								     {  
								     experience=safeParseInt(playerPageLine.substring(16,17));
								     }
								   }  
								 }
						       }
					       }
						 //Got player details, check college exists, if not create it.
						 try
						   {
						   Statement sql=database.createStatement();
						   ResultSet queryResults = sql.executeQuery("SELECT collegeNumber " +
							 									     "FROM   colleges " +
																	 "WHERE  collegeName = '" +
																	 college +
																	 "'");
						   if(queryResults.first())
							 {
							 //Found a college.	
							 collegeNumber=queryResults.getInt(1);
							 }				
						   else
							 {
							 //No college found, create one.
							 int updated=sql.executeUpdate("INSERT INTO colleges (CollegeName,DateTimeStamp) " +
														   "VALUES ('" + college + "','" +
														   Routines.getDateTime(false) + "')" );	
							 if(updated!=1)
							   {
							   Routines.writeToLog(servletName,"Unable to store college : Reason unknown",false,context);
							   session.setAttribute("message","Unable to store college : Reason unknown");
							   errorFound=true;
							   return;
	                           }	                              
							 queryResults = sql.executeQuery("SELECT collegeNumber " +
															 "FROM   colleges " +
															 "WHERE  collegeName = '" +
															 college + "'");
							 if(queryResults.first())
							   {
							   //Found a college.	
							   collegeNumber=queryResults.getInt(1);
							   }	
							 }	   							   
						   }
						 catch(SQLException error)
						   {
						   Routines.writeToLog(servletName,"Unable to get college : " + error,false,context);
						   session.setAttribute("message","Unable to get college : " + error);
						   errorFound=true;
						   return;
	                       } 
	                     if(collegeNumber==0)
	                       {
						   Routines.writeToLog(servletName,"Unable to find college : " + college,false,context);
											   session.setAttribute("message","Unable to find college : " + college);
											   errorFound=true;
											   return;
	                       
	                       }	  
	                     //Check to see if player already exists, if so get his MasterPlayerNumber  
						 try
						   {
						   Statement sql=database.createStatement();
						   ResultSet queryResults = sql.executeQuery("SELECT MasterPlayerNumber " +
																	 "FROM   masterplayers " +
																	 "WHERE  RealNumber = " +
																	 playerNumber + " " +
                                                                     "ORDER BY Season ASC;");
						   if(queryResults.first())
							  {
							  masterPlayerNumber=queryResults.getInt(1);	
							  }				
							 }
						   catch(SQLException error)
							 {
							 Routines.writeToLog(servletName,"Unable to get orginal masterplayer : " + error,false,context);
							 session.setAttribute("message","Unable to get original masterplayer : " + error);
							 errorFound=true;
							 return;
							 } 
						 //Create player.
						 try
						   {
						   Statement sql=database.createStatement();		
						   int updated=sql.executeUpdate("INSERT INTO masterplayers " +
						                                 "(CollegeNumber,PositionNumber,Surname,Forname," +
						                                 "Experience,PreferredNumber,Height,Weight," +
						                                 "DateTimeStamp,RealNumber,MasterPlayerNumber,Season,DOB) " +
														 "VALUES (" + 
														 collegeNumber + "," +
														 positionNumber + ",\"" +
														 surName + "\",\"" +
														 foreName + "\"," +
														 experience + "," +
														 preferredNumber + "," +
														 height + "," +
														 weight + ",'" +
														 Routines.getDateTime(false) + "'," +
														 playerNumber + "," +
														 masterPlayerNumber + "," +
														 year + ",'" +														 (ccyy+"-"+mm+"-"+dd) + "')" );	
						   if(updated!=1)
							 {
							 Routines.writeToLog(servletName,"Unable to store masterplayer : Reason unknown",false,context);
							 session.setAttribute("message","Unable to store masterplayer : Reason unknown");
							 errorFound=true;
							 return;
	                         }	                              
						   }	   							   
						 catch(SQLException error)
						   {
						   Routines.writeToLog(servletName,"Unable to store masterplayer : " + error,false,context);
						   session.setAttribute("message","Unable to store masterplayer : " + error);
						   errorFound=true;
						   return;
	                       } 						   
						 try
						   {
		                   Thread.sleep(10);
		                   }
		                 catch(InterruptedException error)
		                   {
						   Routines.writeToLog(servletName,"Unable to sleep : " + error,false,context);
						   session.setAttribute("message","Unable to sleep : " + error);
						   errorFound=true;
						   return;
	                       }  	
			            }
					  catch(MalformedURLException error)
					    {
					    System.out.println(error.toString());
						session.setAttribute("message",error.toString());
						errorFound=true;
						return;
	                    }
					  catch(IOException error)
						{
						System.out.println(error.toString());
						session.setAttribute("message",error.toString());
						errorFound=true;
						return;
	                    }
			          }
			       }
			     }
			   }
	      }
	   catch(MalformedURLException error)
	      {
		  System.out.println(error.toString());
		  session.setAttribute("message",error.toString());
		  errorFound=true;
		  return;
	      }
	   catch(IOException error)
		  {
		  System.out.println(error.toString());
		  session.setAttribute("message",error.toString());
		  errorFound=true;
		  return;
	      }
		try{
		   Statement sql=database.createStatement();
		   sql.executeUpdate("UPDATE masterplayers SET MasterPlayerNumber=PlayerNumber WHERE MasterPlayerNumber=0;");
		   }
		catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Unable to update masterplayers : " + error,false,context);
		   session.setAttribute("message","Unable to update masterplayers : " + error);
		   errorFound=true;
		   return;
		   }
	   if(!errorFound)
	     {
		 session.setAttribute("message","Player upload complete");	
	     }	  	   
	   }


   static String[] getName(int forNameNumber,
                           int surNameNumber,
                           ServletContext context)
      {
	  String servletName="Routines.getName";	
      String[] returnString=new String[2];
      try
        {
        String file=context.getRealPath("/");
        FileReader forNames=new FileReader(file + "/Data/fornames.dat");
        BufferedReader forNamesBuffer=new BufferedReader(forNames);
        boolean endOfFile=false;
        int recordsRead=0;
        while(!endOfFile)
             {
             String forName=forNamesBuffer.readLine();
             if(forName==null)
               {
               endOfFile=true;
               }
             else
               {
               recordsRead++;
               if(forNameNumber==recordsRead)
                 {
                 returnString[0]=forName;
                 endOfFile=true;
                 }
               }
             }
       forNamesBuffer.close();
        }
      catch(IOException error)
        {
		Routines.writeToLog(servletName,"Problem with fornames file : " + error,false,context);	
        }
      try
        {
        String file=context.getRealPath("/");
        FileReader surNames=new FileReader(file + "/Data/surnames.dat");
        BufferedReader surNamesBuffer=new BufferedReader(surNames);
        boolean endOfFile=false;
        int recordsRead=0;
        while(!endOfFile)
             {
             String surName=surNamesBuffer.readLine();
             if(surName==null)
               {
               endOfFile=true;
               }
             else
               {
               recordsRead++;
               if(surNameNumber==recordsRead)
                 {
                 returnString[1]=surName;
                 endOfFile=true;
                 }
               }
             }
       surNamesBuffer.close();
        }
      catch(IOException error)
        {
		Routines.writeToLog(servletName,"Problem with surnames file : " + error,false,context);	
        }
      return returnString;
      }

   static boolean updateGameBoard(int leagueNumber,
                                  Connection database,
                                  ServletContext context)
      {
	  String servletName="Routines.updateGameBoard";	
	  int[] numOfSkills=new int[16];
	  int positions[]={13,12,15};
	  int season=0;
	  int week=0;
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT Season,Week " +
									   "FROM leagues " +
									   "WHERE LeagueNumber = " + leagueNumber);
		if(queryResult.first())
		  {
		  season=queryResult.getInt(1);
		  week=queryResult.getInt(2);	
          week++;	
		  }
		else
		  {
		  Routines.writeToLog(servletName,"league not found : " + leagueNumber,false,context);	
		  }	  								   
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);
		}
	  for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
	     {	
	     try
		   {
		   Statement sql=database.createStatement();
		   ResultSet queryResult;		
		   queryResult=sql.executeQuery("SELECT COUNT(positionskills.Sequence) " +
									    "FROM positionskills,positions " +
									    "WHERE Type!=3 " +
									    "AND RealPosition=1 " +
									    "AND positionskills.PositionNumber=" + positions[currentPosition] + " " +
			                            "AND positionskills.PositionNumber=positions.PositionNumber " +
									    "GROUP BY positions.PositionNumber ");
		    if(queryResult.next())
		      {
		      numOfSkills[positions[currentPosition]]=queryResult.getInt(1);
		      }
		    }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Database error retrieving positionskills : " + error,false,context);
		  }
	     }  
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT HomeTeam,AwayTeam " +
								     "FROM fixtures " +
								     "WHERE LeagueNumber = " + leagueNumber + " " +
								     "AND Season = " + season + " " +
								     "AND Week = " + week + " " +
								     "ORDER BY Game ASC");
		while(queryResult.next())
		   {
		   int[] teams=new int[2];	
		   teams[0]=queryResult.getInt(1);
		   teams[1]=queryResult.getInt(2);
           for(int currentTeam=0;currentTeam<2;currentTeam++)
              {		   
			 int sortedPlayers[]=new int[30];
			 int masterCurrentPlayer=0;
			 for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
			    {	
			    int unSortedPlayers[][]=new int[10][27];
			    int currentPlayer=0;
			    try
			      {
				  Statement sql2=database.createStatement();
				  ResultSet queryResult2;
				  int tempTeam=0;
				  if(currentTeam==0)
				    {
				    tempTeam=1;	
				    }
			      queryResult2=sql2.executeQuery("SELECT PlayerNumber," +
											     "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
											     "Skill1,Skill2,Skill3,Skill4,Skill5," +
											     "Skill6,Skill7,Skill8,Skill9,Skill10," +
											     "Skill11,Skill12,Skill13,Skill14,Skill15," +
											     "Skill16,Skill17,Skill18,Skill19,Skill20 " +
											     "FROM players " +
											     "WHERE TeamNumber=" + teams[tempTeam] + " " +
											     "AND players.PositionNumber=" + positions[currentPosition]);
				  while(queryResult2.next())
				     {
					 int playerNumber=queryResult2.getInt(1);
					 int positionNumber=positions[currentPosition];
					 int skills[]=new int[25];
					 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
						{
						skills[currentSkill]=queryResult2.getInt(2+currentSkill);
						}
					 unSortedPlayers[currentPlayer][0]=playerNumber;
					 unSortedPlayers[currentPlayer][1]=positionNumber;
					 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
						{
						unSortedPlayers[currentPlayer][2+currentSkill]=skills[currentSkill];
						}
					 currentPlayer++;
					 }
				  int tempSortedPlayers[][]=Routines.sortPlayers(positions[currentPosition],
															 unSortedPlayers,
															 numOfSkills,
															 1,
															 1,
															 database,
															 context);
				  for(currentPlayer=0;currentPlayer<10;currentPlayer++)
				     {
				     if(tempSortedPlayers[currentPlayer][0]!=0)
				       {
				       sortedPlayers[masterCurrentPlayer]=tempSortedPlayers[currentPlayer][0];
				       if(currentPlayer>19)
				         {
						 Routines.writeToLog(servletName,"gameboard filled, increase receivers",false,context);	
				         }
				       masterCurrentPlayer++;	
				       }
				     else
				       {
				       currentPlayer=10;	
				       }	  		
				     }												 
			      }							     
				catch(SQLException error)
				  {
				  Routines.writeToLog(servletName,"Unable to retrieve players : " + error,false,context);	
				  }
                }
			  try
				{
				Statement sql2=database.createStatement();
				int updated=sql2.executeUpdate("UPDATE gameboard " +
                                               "SET Receiver01=" + sortedPlayers[0] + 
                                               ",Receiver02=" + sortedPlayers[1] +
					                           ",Receiver03=" + sortedPlayers[2] +
					                           ",Receiver04=" + sortedPlayers[3] +
					                           ",Receiver05=" + sortedPlayers[4] +
					                           ",Receiver06=" + sortedPlayers[5] +
					                           ",Receiver07=" + sortedPlayers[6] +
					                           ",Receiver08=" + sortedPlayers[7] +
					                           ",Receiver09=" + sortedPlayers[8] +
					                           ",Receiver10=" + sortedPlayers[9] +
					                           ",Receiver11=" + sortedPlayers[10] +
					                           ",Receiver12=" + sortedPlayers[11] +
					                           ",Receiver13=" + sortedPlayers[12] +
					                           ",Receiver14=" + sortedPlayers[13] +
					                           ",Receiver15=" + sortedPlayers[14] +
					                           ",Receiver16=" + sortedPlayers[15] +
					                           ",Receiver17=" + sortedPlayers[16] +
					                           ",Receiver18=" + sortedPlayers[17] +
					                           ",Receiver19=" + sortedPlayers[18] +
					                           ",Receiver20=" + sortedPlayers[19] +
					                           ",DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                               "WHERE TeamNumber=" + teams[currentTeam]);
				if(updated!=1)
				  {
				  Routines.writeToLog(servletName,"gameboard not updated",false,context);
				  }
				}
			  catch(SQLException error)
				{
				Routines.writeToLog(servletName,"Database error updating gameboard : " + error,false,context);
				}
              }
		   }							       
	    }
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);
		}
      return true;	
      }	                               

   static boolean createDraftPriorities(int teamNumber,
                                        int leagueNumber,
                                        Connection database,
	                                    ServletContext context)
      {
	  String servletName="Routines.createDraftPriorities";	
      int numOfPositions=0;
      int[] positionNumbers=null;
      // Retrieve positions.
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResponse;
        sql.executeUpdate("DELETE FROM draftpriorities " +
                          "WHERE TeamNumber=" + teamNumber);
        queryResponse=sql.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "WHERE Type!=3 " +
                                       "AND RealPosition=1");
        while(queryResponse.next())
             {
             numOfPositions++;
             }
        positionNumbers=new int[numOfPositions];
        queryResponse=sql.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "WHERE Type!=3 " +
                                       "AND RealPosition=1");
        int currentPosition=0;
        while(queryResponse.next())
             {
             positionNumbers[currentPosition]=queryResponse.getInt(1);
             currentPosition++;
             }
        // Create default Staff Draft Priorities.
        int[] sequence=new int[numOfPositions];
        Random random = new Random(teamNumber);
        for(currentPosition=0;currentPosition<numOfPositions;currentPosition++)
           {
           boolean notUsed=false;
           while(!notUsed)
                {
                int randomNumber=((int)(random.nextDouble()*numOfPositions)+1);
                notUsed=true;
                for(int currentPosition2=0;currentPosition2<numOfPositions;currentPosition2++)
                   {
                   if(sequence[currentPosition2]==randomNumber)
                     {
                     notUsed=false;
                     }
                   }
                if(notUsed)
                  {
                  sequence[currentPosition]=randomNumber;
                  }
                }
           }
        for(currentPosition=0;currentPosition<numOfPositions;currentPosition++)
           {
           sql.executeUpdate("INSERT INTO draftpriorities (" +
                             "LeagueNumber,TeamNumber,Sequence,PositionNumber,DateTimeStamp)" +
                             " VALUES (" +
                             leagueNumber +
                             "," +
                             teamNumber +
                             "," +
                             sequence[currentPosition] +
                             "," +
                             positionNumbers[currentPosition] +
                             ",'" +
                             Routines.getDateTime(false) +
                             "')");
           }
        }
     catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Error creating draftpriorities : " + error,false,context);	
        return false;
        }
      return true;
      }

   static boolean processStaffSignings(int leagueNumber,
                                       Connection database,
                                       ServletContext context,
                                       Properties properties)
      {
	  String servletName="Routines.processStaffSignings";	
      Routines.writeToLog(servletName,"Entered processStaffSignings",false,context);
      final int[] skills={95,90,85,80,75,70,65,60,55,50,45,40,35,30,25,20,15,10,5};
      final int[] salaries={2000000,1900000,1800000,1700000,1600000,1500000,1400000,1300000,1200000,1100000,1000000,
                            900000,800000,700000,600000,500000,400000,300000,200000};
      boolean success=true;
      synchronized(dbLock)
         {
         int numOfColleges=0;
         int numberOfNames[]=new int[2];
         numberOfNames=Routines.getNumOfNames(context);
         // Retrieve latest collegeNumber;
         try
           {
           Statement sql=database.createStatement();
           ResultSet queryResult;
           numOfColleges=0;
           queryResult=sql.executeQuery("SELECT   CollegeNumber " +
                                        "FROM     colleges " +
                                        "ORDER BY CollegeNumber DESC");
           if(queryResult.first())
             {
             numOfColleges=queryResult.getInt(1);
             }
           }
         catch(SQLException error)
           {
           Routines.writeToLog(servletName,"Unable to retrieve college numbers" + error,false,context);
           return false;
           }
         try
           {
           Statement sql=database.createStatement();
           Statement sql2=database.createStatement();
           ResultSet queryResult1;
           ResultSet queryResult2;
           int currentTeamNumber=0;
           int teamNumber=0;
           Random random=null;
           queryResult1=sql.executeQuery("SELECT draftpriorities.TeamNumber,PositionNumber,Sequence,DraftPriorityNumber,draftpriorities.DateTimeStamp " +
                                         "FROM draftpriorities,teams " +
                                         "WHERE LeagueNumber = " + leagueNumber + " " +
                                         "AND draftpriorities.TeamNumber=teams.TeamNumber " +
                                         "AND Status=0 " +
                                         "ORDER BY draftpriorities.TeamNumber ASC,draftpriorities.Sequence ASC");
           while(queryResult1.next())
                {
                teamNumber=queryResult1.getInt(1);
                if(teamNumber!=currentTeamNumber)
                  {
//                  database.close();
//                  database=null;
//                  try
//                    {
//                    database=DriverManager.getConnection(properties.getProperty("connection.url"),properties);
//                    }
//                  catch(SQLException error)
//                    {
//                    Routines.writeToLog(servletName,"Error connecting to database : " + error,false,context);
//                    }
                  sql=database.createStatement();
                  sql2=database.createStatement();
                  random=new Random(teamNumber);
                  currentTeamNumber=teamNumber;
                  }
                int positionNumber=queryResult1.getInt(2);
                int sequence=queryResult1.getInt(3);
                int draftPriorityNumber=queryResult1.getInt(4);
                String dateTimeStamp=queryResult1.getString(5);
                sequence--;
                int forNameNumber=((int)(random.nextDouble()*numberOfNames[0])+1);
                int surNameNumber=((int)(random.nextDouble()*numberOfNames[1])+1);
                int collegeNumber=((int)(random.nextDouble()*numOfColleges)+1);
                int motivationIntelligence=(int)(random.nextDouble()*101);
                int motivationSuccess=(int)(random.nextDouble()*101);
                int motivationMoney=(int)(random.nextDouble()*101);
                int motivationTheGame=(int)(random.nextDouble()*101);
                int intelligence=(int)(random.nextDouble()*101);
                int ego=(int)(random.nextDouble()*101);
                int attitude=(int)(random.nextDouble()*101);
                int potential=(int)(random.nextDouble()*11);
                int burnRate=((int)(random.nextDouble()*10))+1;
                int height=((int)(random.nextDouble()*15))+66;
                int weight=height*3;
                int weightAdjust=((int)(random.nextDouble()*84));
                if(weightAdjust<42)
                  {
                  weight=weight-=(weightAdjust/2);
                  }
                else
                  {
                  weight=weight+=(weightAdjust/2);
                  }
                String[] name=new String[2];
                name=Routines.getName(forNameNumber,
                                      surNameNumber,
                                      context);
                 queryResult2=sql2.executeQuery("SELECT PlayerNumber " +
                                                "FROM players " +
                                                "WHERE TeamNumber=" + teamNumber + " " +
                                                "AND PositionNumber=" + positionNumber);
                 if(!queryResult2.first())
                   {
                   int updated=sql2.executeUpdate("INSERT INTO players " +
                                                  "(WorldNumber," +
                                                  "TeamNumber,CollegeNumber," +
                                                  "PositionNumber,Surname," +
                                                  "Forname,Height," +
                                                  "Weight,MotivationIntelligence," +
                                                  "MotivationSuccess,MotivationMoney," +
                                                  "MotivationTheGame,Intelligence," +
                                                  "Ego,Attitude,Potential,BurnRate," +
                                                  "Skill1,ContractValue,ContractLength,DateTimeStamp) VALUES (" +
                                                  leagueNumber + "," +
                                                  teamNumber + "," +
                                                  collegeNumber + "," +
                                                  positionNumber + ",\"" +
                                                  name[1] + "\",\"" +
                                                  name[0] + "\"," +
                                                  height + "," +
                                                  weight + "," +
                                                  motivationIntelligence + "," +
                                                  motivationSuccess + "," +
                                                  motivationMoney + "," +
                                                  motivationTheGame + "," +
                                                  intelligence + "," +
                                                  ego + "," +
                                                  attitude + "," +
                                                  potential + "," +
                                                  burnRate + "," +
                                                  skills[sequence] + "," +
                                                  salaries[sequence] + ",1,'" +
                                                  Routines.getDateTime(false) + "')");
                  if(updated!=1)
                    {
                    Routines.writeToLog(servletName,"Staff not created,reason unknown",false,context);
                    return false;
                    }
                  }
                int updated=sql.executeUpdate("DELETE FROM draftpriorities " +
                                              "WHERE DraftPriorityNumber=" + draftPriorityNumber);
                if(updated!=1)
                  {
                  Routines.writeToLog(servletName,"draftpriorities not deleted (" + draftPriorityNumber + ")",false,context);
                  }
                }
          Routines.writeToLog(servletName,"Starting createDraftPriorities",false,context);
          queryResult1=sql.executeQuery("SELECT teams.TeamNumber " +
                                        "FROM conferences,divisions,leagueteams,teams " +
                                        "WHERE conferences.LeagueNumber=" + leagueNumber + " " +
                                        "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
                                        "AND divisions.DivisionNumber=leagueteams.DivisionNumber " +
                                        "AND leagueteams.TeamNumber=teams.TeamNumber " +
                                        "AND Status=0");
          while(queryResult1.next())
               {
               teamNumber=queryResult1.getInt(1);
//               database.close();
//               database=null;
//               try
//                 {
//                 database=DriverManager.getConnection(properties.getProperty("connection.url"),properties);
//                 }
//               catch(SQLException error)
//                 {
//                  Routines.writeToLog(servletName,"Error connecting to database : " + error,false,context);
//                 }
               sql=database.createStatement();
               sql2=database.createStatement();
               if(!Routines.createDraftPriorities(teamNumber,leagueNumber,database,context))
                 {
                 Routines.writeToLog(servletName,"Team not updated (TeamNumber=" + teamNumber +"),reason unknown",false,context);
                 return false;
                 }
               if(!createDraftRatings(leagueNumber,
                                      teamNumber,
                                      false,
                                      null,
                                      database,
                                      context))
                 {
                 Routines.writeToLog(servletName,"createDraftRatings failed (TeamNumber=" + teamNumber + ")",false,context);
                 return false;
                 }
               int updated=sql2.executeUpdate("UPDATE teams SET Status=1 " +
                                              "WHERE TeamNumber=" + teamNumber);
               if(updated!=1)
                 {
                 Routines.writeToLog(servletName,"Team not updated (teamNumber=" + teamNumber + "), reason unknown",false,context);
                 return false;
                 }
              }
           }
         catch(SQLException error)
           {
           Routines.writeToLog(servletName,"Unable to create staff : " + error,false,context);
           return false;
           }
        }
      Routines.writeToLog(servletName,"Ending createDraftPriorities",false,context);
      return success;
      }

   static boolean createDraftRatings(int leagueNumber,
                                     int teamNumber,
                                     boolean useSession,
                                     HttpSession session,
                                     Connection database,
                                     ServletContext context)
      {
	  String servletName="Routines.createDraftRatings";	
      try
        {
        Statement sql1=database.createStatement();
        Statement sql2=database.createStatement();
        ResultSet queryResult1;
        ResultSet queryResult2;
        if(teamNumber!=0)
          {
          sql1.executeUpdate("DELETE FROM draftratings " +
                             "WHERE TeamNumber=" + teamNumber);
          }                   
		// Retrieve latest playerSeason;
		int playerSeason=0;
		queryResult1 = sql1.executeQuery("SELECT   (Value+2000) " +
										   "FROM     gamesettings " +
										   "WHERE    SettingName='Player Season'");
		if(queryResult1.first())
		  {
		  playerSeason = queryResult1.getInt(1);
		  }
		queryResult1=sql1.executeQuery("SELECT COUNT(PlayerNumber) " +
									   "FROM masterplayers " +
									   "WHERE Season=" + playerSeason + " " +
									   "AND Experience=0");
		int numOfPlayers=0;
		  if(queryResult1.first())
			{
			numOfPlayers=queryResult1.getInt(1);
			}
		String[] stringRatings=new String[numOfPlayers];
        int currentPlayer=0;
        queryResult1=sql1.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "WHERE RealPosition=1 " +
                                       "ORDER BY PositionNumber ASC");
        while(queryResult1.next())
             {
             int positionNumber=queryResult1.getInt(1);
             int numOfSkills=0;
             int ratings=0;
             queryResult2=sql2.executeQuery("SELECT Sequence " +
                                            "FROM positionskills " +
                                            "WHERE PositionNumber=" + positionNumber + " " +
                                            "ORDER BY Sequence DESC");
             if(queryResult2.first())
               {
               numOfSkills+=queryResult2.getInt(1);
               }
             queryResult2=sql2.executeQuery("SELECT players.PlayerNumber," +
                                            "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                            "Skill6,Skill7,Skill8,Skill9,Skill10," +
                                            "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                            "Skill16,Skill17,Skill18,Skill19,Skill20 " +
                                            "FROM players,masterplayers " +
                                            "WHERE WorldNumber=" + leagueNumber + " " +
                                            "AND PositionNumber=" + positionNumber + " " +
                                            "AND Experience=0 " +
                                            "AND players.MasterPlayerNumber=masterplayers.MasterPlayerNumber " +
                                            "ORDER BY PositionNumber ASC");
             while(queryResult2.next())
                  {
				  ratings=0;
                  int[] realSkills=new int[20];
                  int playerNumber=queryResult2.getInt(1);
                  for(int currentSkill=0;currentSkill<realSkills.length;currentSkill++)
                     {
                     realSkills[currentSkill]=queryResult2.getInt(currentSkill+2);
                     }
                  for(int currentSkill=0;currentSkill<numOfSkills;currentSkill++)
                     {
                     ratings+=realSkills[currentSkill];
                     }
                  ratings=(((ratings/numOfSkills)+5)/10)*10;
                  if(ratings>100||ratings<0)
                    {
                    for(int currentSkill=5;currentSkill<numOfSkills;currentSkill++)
                       {
                       Routines.writeToLog(servletName,"realSkills["+currentSkill+"]="+realSkills[currentSkill],false,context);
                       }
                    }
                  String stringPlayerNumber=String.valueOf(playerNumber);
                  String stringPositionNumber=String.valueOf(positionNumber);
                  switch((String.valueOf(ratings)).length())
                         {
                         case 0:
                            stringRatings[currentPlayer]="0000";
                            break;
       	                 case 1:
                            stringRatings[currentPlayer]="000"+ratings;
  	                    break;
                         case 2:
                            stringRatings[currentPlayer]="00"+ratings;
  	                    break;
                         case 3:
                            stringRatings[currentPlayer]="0"+ratings;
  	                    break;
                         case 4:
                            stringRatings[currentPlayer]=String.valueOf(ratings);
  	                    break;
                         default:
                            if(useSession)
                              {
                              session.setAttribute("message","Unable to pad skills");
                              }
                            else
                              {
                              Routines.writeToLog(servletName,"Unable to pad skills",false,context);
                              }
                            return false;
                         }
                  switch(stringPlayerNumber.length())
                         {
       	                 case 1:
                            stringRatings[currentPlayer]+="0000000000"+stringPlayerNumber;
  	                    break;
                         case 2:
                            stringRatings[currentPlayer]+="000000000"+stringPlayerNumber;
  	                    break;
                         case 3:
                            stringRatings[currentPlayer]+="00000000"+stringPlayerNumber;
  	                    break;
                         case 4:
                            stringRatings[currentPlayer]+="0000000"+stringPlayerNumber;
  	                    break;
                         case 5:
                            stringRatings[currentPlayer]+="000000"+stringPlayerNumber;
  	                    break;
                         case 6:
                            stringRatings[currentPlayer]+="00000"+stringPlayerNumber;
  	                    break;
                         case 7:
                            stringRatings[currentPlayer]+="0000"+stringPlayerNumber;
  	                    break;
                         case 8:
                            stringRatings[currentPlayer]+="000"+stringPlayerNumber;
  	                    break;
                         case 9:
                            stringRatings[currentPlayer]+="00"+stringPlayerNumber;
  	                    break;
                         case 10:
                            stringRatings[currentPlayer]+="0"+stringPlayerNumber;
  	                    break;
                         case 11:
                            stringRatings[currentPlayer]+=stringPlayerNumber;
  	                    break;
                         default:
                            if(useSession)
                              {
                              session.setAttribute("message","Unable to pad playerNumber");
                              }
                            else
                              {
                              Routines.writeToLog(servletName,"Unable to pad playerNumber",false,context);
                              }
                            return false;
                         }
                  switch(stringPositionNumber.length())
                         {
       	                 case 1:
                            stringRatings[currentPlayer]+="00"+stringPositionNumber;
  	                    break;
                         case 2:
                            stringRatings[currentPlayer]+="0"+stringPositionNumber;
  	                    break;
                         case 3:
                            stringRatings[currentPlayer]+=stringPositionNumber;
  	                    break;
                         default:
                            if(useSession)
                              {
                              session.setAttribute("message","Unable to pad positionNumber");
                              }
                            else
                              {
                              Routines.writeToLog(servletName,"Unable to pad positionNumber",false,context);
                              }
                            return false;
                         }
                  System.out.println("string["+currentPlayer+"]="+stringRatings[currentPlayer]);       
                  currentPlayer++;
                  }
             }
             if(currentPlayer>0)
               { 
               Arrays.sort(stringRatings);
               int positionSequences[]=new int[100];
               int rating=0;
               for(int currentRating=stringRatings.length-1;currentRating>-1;currentRating--)
                  {
                  rating++;
                  int player=safeParseInt(stringRatings[currentRating].substring(5,15));
                  int position=safeParseInt(stringRatings[currentRating].substring(16,18));
                  positionSequences[position]++;
                  int updated=sql1.executeUpdate("INSERT INTO draftratings " +
                                                 "(LeagueNumber,PlayerNumber,TeamNumber," +
                                                 "PositionRating,OverallRating,DateTimeStamp) VALUES (" +
                                                 leagueNumber + "," +
                                                 player + "," +
                                                 teamNumber + "," +
                                                 positionSequences[position] + "," +
                                                 rating + ",'" +
                                                 getDateTime(false) + "')");
                  if(updated!=1)
                    {
                    Routines.writeToLog(servletName,"Unable to create draftratings",false,context);
                    return false;
                    }
                  }
               }  
          }
        catch(SQLException error)
          {
        if(useSession)
          {
          session.setAttribute("message","Unable to find positions entries : " + error);
          }
        else
          {
          Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);
          }
        return false;
        }
      return true;
      }

	static int getTotalSkills(int teamNumber,
							  int playerNumber,
							  Connection database,
							  ServletContext context)
	   {
	 String servletName="Routines.getTotalSkills";	
	 int[] numOfSkills=getNumOfSkills(database,context);
	 int positionNumber=0;
	 int totalSkill=0;
	 int skills[]=new int[25];
	 try
	   {
	   int[] positionSkills=Routines.getNumOfSkills(database,context);
	   Statement sql=database.createStatement();
	   ResultSet queryResult;
	   queryResult=sql.executeQuery("SELECT PositionNumber " +
									"Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
									"Skill1,Skill2,Skill3,Skill4,Skill5," +
									"Skill6,Skill7,Skill8,Skill9,Skill10," +
									"Skill11,Skill12,Skill13,Skill14,Skill15," +
									"Skill16,Skill17,Skill18,Skill19,Skill20 " +
									"FROM players,masterplayers " +
									"WHERE PlayerNumber=" + playerNumber + " " +
									"players.MasterPlayerNumber=masterplayers.MasterPlayerNumber");
	   if(queryResult.next())
		 {
		 positionNumber=queryResult.getInt(1);
		 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
			{
			skills[currentSkill]=queryResult.getInt(2+currentSkill);	
			}	
		 }
	   }
	 catch(SQLException error)
	   {
	   Routines.writeToLog(servletName,"Unable to retrieve player : " + error,false,context);	
	   }	
	 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
		{
		totalSkill+=skills[currentSkill];	
		}
	   return totalSkill;	
	   }	


//   static int getTotalMaskedSkills(int teamNumber,
//                                   int playerNumber,
//                                   Connection database,
//                                   ServletContext context)
//      {
//	  String servletName="Routines.getTotalMaskedSkills";	
//	  int[] numOfSkills=getNumOfSkills(database,context);
//	  int scoutNumber=0;
//	  int scoutSkills=0;
//	  int positionNumber=0;
//      int totalSkill=0;
//	  int skills[]=new int[25];
//	  try
//		{
//		int[] positionSkills=Routines.getNumOfSkills(database,context);
//		Statement sql=database.createStatement();
//		ResultSet queryResult;
//		queryResult=sql.executeQuery("SELECT PlayerNumber,Skill1 " +
//									 "FROM players " +
//									 "WHERE TeamNumber=" + teamNumber + " " +
//									 "AND PositionNumber=39");
//		if(queryResult.next())
//		  {
//		  scoutNumber=queryResult.getInt(1);
//		  scoutSkills=queryResult.getInt(2);
//		  }
//		}
//	  catch(SQLException error)
//		{
//		Routines.writeToLog(servletName,"Unable to retrieve scout : " + error,false,context);	
//		}		 
//	  try
//		{
//		int[] positionSkills=Routines.getNumOfSkills(database,context);
//		Statement sql=database.createStatement();
//		ResultSet queryResult;
//		queryResult=sql.executeQuery("SELECT PositionNumber " +
//		                             "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
//									 "Skill1,Skill2,Skill3,Skill4,Skill5," +
//									 "Skill6,Skill7,Skill8,Skill9,Skill10," +
//									 "Skill11,Skill12,Skill13,Skill14,Skill15," +
//									 "Skill16,Skill17,Skill18,Skill19,Skill20 " +
//							         "FROM players " +
//									 "WHERE PlayerNumber=" + playerNumber);
//		if(queryResult.next())
//		  {
//		  positionNumber=queryResult.getInt(1);
//		  for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
//		     {
//		     skills[currentSkill]=queryResult.getInt(2+currentSkill);	
//		     }	
//		  }
//		}
//	  catch(SQLException error)
//		{
//		Routines.writeToLog(servletName,"Unable to retrieve player : " + error,false,context);	
//		}	
//	  for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
//	     {
//	     totalSkill+=skills[currentSkill];	
//	     }
//      return totalSkill;	
//      }	

//   static int[] getMaskedSkills(int scoutNumber,
//                                int scoutSkills,
//                                int playerNumber,
//                                int numOfSkills,
//                                int[] skills)
//      {
//      int returnSkills[]=skills;
//      int workingScoutSkills=scoutSkills+5;
//      workingScoutSkills=workingScoutSkills/10;
//      int[] mask={44,40,36,32,28,24,20,16,12,8,4};
//      Random random=new Random(scoutNumber+playerNumber);
//      for(int currentSkill=0;currentSkill<numOfSkills+5;currentSkill++)
//         {
//         int randomNumber=((int)(random.nextDouble()*(mask[workingScoutSkills]*2)));
//         randomNumber++;
//         if(randomNumber/2>mask[workingScoutSkills])
//           {
//           returnSkills[currentSkill]+=randomNumber/2;
//           }
//         if(randomNumber/2<mask[workingScoutSkills])
//           {
//           returnSkills[currentSkill]-=randomNumber/2;
//           }
//         if(returnSkills[currentSkill]<0)
//           {
//           returnSkills[currentSkill]=0;
//           }
//         if(returnSkills[currentSkill]>100)
//           {
//           returnSkills[currentSkill]=100;
//           }
//         }
//      return returnSkills;
//      }

   static String[] getTitleHeaders(int positionNumber,
                                   int selection,
	                               ServletContext context)
      {
	  String servletName="Routines.getTitleHeaders";	
      String[] returnString=new String[5];
      if(selection==1)
        {
        if(positionNumber!=0)
          {
          returnString[0]="AllSkills";
          returnString[1]="Potential";
          returnString[2]="Personal";
          }
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnString[0]="AllSkills";
                 returnString[1]="Potential";
                 returnString[2]="Personal";
                 returnString[3]="";
                 returnString[4]="";
                 break;
              case 1:
                 //Kicker
                 returnString[3]="Accuracy";
                 returnString[4]="KickOff";
	         break;
              case 2:
                 //Punter
                 returnString[3]="Strength";
                 returnString[4]="HangTime";
	         break;
              case 7:
                 //Centre
                 returnString[3]="RunBlock";
                 returnString[4]="PassBlock";
	         break;
              case 8:
                 //Left Guard
                 returnString[3]="RunBlock";
                 returnString[4]="PassBlock";
	         break;
              case 9:
                 //Right Guard
                 returnString[3]="RunBlock";
                 returnString[4]="PassBlock";
	         break;
              case 10:
                 //Left Tackle
                 returnString[3]="RunBlock";
                 returnString[4]="PassBlock";
	         break;
              case 11:
                 //Right Tackle
                 returnString[3]="RunBlock";
                 returnString[4]="PassBlock";
	         break;
              case 12:
                 //Tight End
                 returnString[3]="Receiving";
                 returnString[4]="Blocking";
	         break;
              case 13:
                 //Wide Receiver
                 returnString[3]="Receiving";
                 returnString[4]="Blocking";
	         break;
              case 14:
                 //Quarterback
                 returnString[3]="Passing";
                 returnString[4]="Running";
	         break;
              case 15:
                 //Running Back
                 returnString[3]="Running";
                 returnString[4]="Receiving";
	         break;
              case 17:
                 //Defensive End
                 returnString[3]="PassRush";
                 returnString[4]="Tackling";
	         break;
              case 18:
                 //Defensive Tackle
                 returnString[3]="Tackling";
                 returnString[4]="PassRush";
	         break;
              case 19:
                 //Middle Linebacker
                 returnString[3]="Tackling";
                 returnString[4]="Coverage";
	         break;
              case 20:
                 //Outside Linebacker
                 returnString[3]="Tackling";
                 returnString[4]="Coverage";
	         break;
              case 21:
                 //Cornerback
                 returnString[3]="Coverage";
                 returnString[4]="Tackling";
	         break;
              case 26:
                 //Free Safety
                 returnString[3]="Coverage";
                 returnString[4]="Tackling";
	         break;
              case 27:
                 //Strong Safety
                 returnString[3]="Coverage";
                 returnString[4]="Tackling";
	         break;
	      default:
                 returnString[3]="";
                 returnString[4]="";
				 Routines.writeToLog(servletName,"Titles not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context);
	       }
        }
      if(selection==2)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
                 break;
              case 1:
                 //Kicker
                 returnString[0]="<25yds";
                 returnString[1]="<35yds";
                 returnString[2]="<45yds";
                 returnString[3]="<50yds";
                 returnString[4]=">50yds";
	         break;
              case 2:
                 //Punter
                 returnString[0]="Blocked";
                 returnString[1]="Endurance";
                 returnString[2]="Injuries";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 7:
                 //Centre
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 8:
                 //Left Guard
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 9:
                 //Right Guard
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 10:
                 //Left Tackle
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 11:
                 //Right Tackle
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 12:
                 //Tight End
                 returnString[0]="QuickRoute";
                 returnString[1]="ShortRoute";
                 returnString[2]="LongRoute";
                 returnString[3]="Hands";
                 returnString[4]="Reverse";
             break;
              case 13:
                 //Wide Receiver
                 returnString[0]="QuickRoute";
                 returnString[1]="ShortRoute";
                 returnString[2]="LongRoute";
				 returnString[3]="Hands";
                 returnString[4]="KickReturn";
             break;
              case 14:
                 //Quarterback
                 returnString[0]="QuickPass";
                 returnString[1]="ShortPass";
                 returnString[2]="LongPass";
                 returnString[3]="QuickInts";
                 returnString[4]="ShortInts";
	         break;
              case 15:
                 //Running Back
                 returnString[0]="InsideRun";
                 returnString[1]="OutsideRun";
                 returnString[2]="Agility";
                 returnString[3]="Speed";
                 returnString[4]="QuickRoute";
	         break;
              case 17:
                 //Defensive End
                 returnString[0]="PassDefl";
                 returnString[1]="FumbleRec";
                 returnString[2]="Endurance";
                 returnString[3]="Injuries";
                 returnString[4]="";
	         break;
              case 18:
                 //Defensive Tackle
                 returnString[0]="PassDefl";
                 returnString[1]="FumbleRec";
                 returnString[2]="Endurance";
                 returnString[3]="Injuries";
                 returnString[4]="";
	         break;
              case 19:
                 //Middle Linebacker
                 returnString[0]="PassRush";
                 returnString[1]="FumbleRec";
                 returnString[2]="Ints";
                 returnString[3]="Endurance";
                 returnString[4]="Injuries";
	         break;
              case 20:
                 //Outside Linebacker
                 returnString[0]="PassRush";
                 returnString[1]="FumbleRec";
                 returnString[2]="Ints";
                 returnString[3]="Endurance";
                 returnString[4]="Injuries";
	         break;
              case 21:
                 //Cornerback
                 returnString[0]="Ints";
                 returnString[1]="PassRush";
                 returnString[2]="FumbleRec";
                 returnString[3]="KickReturn";
                 returnString[4]="PuntReturn";
	         break;
              case 26:
                 //Free Safety
                 returnString[0]="Ints";
                 returnString[1]="PassRush";
                 returnString[2]="FumbleRec";
                 returnString[3]="KickReturn";
                 returnString[4]="PuntReturn";
	         break;
              case 27:
                 //Strong Safety
                 returnString[0]="Ints";
                 returnString[1]="PassRush";
                 returnString[2]="FumbleRec";
                 returnString[3]="KickReturn";
                 returnString[4]="PuntReturn";
	         break;
	      default:
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
				 Routines.writeToLog(servletName,"Titles not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context);
	       }
        }
      if(selection==3)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
                 break;
              case 1:
                 //Kicker
                 returnString[0]="Blocks";
                 returnString[1]="ExtraPoint";
                 returnString[2]="Endurance";
                 returnString[3]="Injuries";
                 returnString[4]="";
	         break;
              case 2:
                 //Punter
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 7:
                 //Centre
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 8:
                 //Left Guard
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 9:
                 //Right Guard
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 10:
                 //Left Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 11:
                 //Right Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 12:
                 //Tight End
				 returnString[0]="Fumbles";
                 returnString[1]="Endurance";
                 returnString[2]="Injuries";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 13:
                 //Wide Receiver
				 returnString[0]="PuntReturn";
                 returnString[1]="Reverse";
                 returnString[2]="Fumbles";
                 returnString[3]="Endurance";
                 returnString[4]="Injuries";
	         break;
              case 14:
                 //Quarterback
                 returnString[0]="LongInts";
                 returnString[1]="PassRush";
                 returnString[2]="Fumbles";
                 returnString[3]="Endurance";
                 returnString[4]="Injuries";
	         break;
              case 15:
                 //Running Back
                 returnString[0]="ShortRoute";
                 returnString[1]="LongRoute";
                 returnString[2]="Hands";
                 returnString[3]="Blocking";
                 returnString[4]="KickReturn";
             break;
              case 17:
                 //Defensive End
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 18:
                 //Defensive Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 19:
                 //Middle Linebacker
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 20:
                 //Outside Linebacker
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 21:
                 //Cornerback
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 26:
                 //Free Safety
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 27:
                 //Strong Safety
                 returnString[0]="Endurance";
                 returnString[1]="Injuries";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
	      default:
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
				 Routines.writeToLog(servletName,"Titles not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context);
 	       }
        }
      if(selection==4)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
                 break;
              case 1:
                 //Kicker
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 2:
                 //Punter
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 7:
                 //Centre
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 8:
                 //Left Guard
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 9:
                 //Right Guard
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 10:
                 //Left Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 11:
                 //Right Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 12:
                 //Tight End
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 13:
                 //Wide Receiver
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 14:
                 //Quarterback
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 15:
                 //Running Back
				 returnString[0]="PuntReturn";
                 returnString[1]="Fumbles";
                 returnString[2]="Endurance";
                 returnString[3]="Injuries";
                 returnString[4]="";
	         break;
              case 17:
                 //Defensive End
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 18:
                 //Defensive Tackle
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 19:
                 //Middle Linebacker
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 20:
                 //Outside Linebacker
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 21:
                 //Cornerback
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 26:
                 //Free Safety
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
              case 27:
                 //Strong Safety
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
	         break;
	      default:
                 returnString[0]="";
                 returnString[1]="";
                 returnString[2]="";
                 returnString[3]="";
                 returnString[4]="";
				 Routines.writeToLog(servletName,"Titles not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context);
	       }
        }
      return returnString;
      }

   static int getSkillsButtons(int positionNumber,
                               ServletContext context)
      {
	  String servletName="Routines.getSkillsButtons";	
      int returnInt=0;
      switch(positionNumber)
            {
            case 0:
               //All Positions;
               returnInt=1;
               break;
            case 1:
               //Kicker
               returnInt=3;
               break;
            case 2:
               //Punter
               returnInt=2;
               break;
            case 7:
               //Centre
               returnInt=2;
               break;
            case 8:
               //Left Guard
               returnInt=2;
               break;
            case 9:
               //Right Guard
               returnInt=2;
               break;
            case 10:
               //Left Tackle
               returnInt=2;
               break;
            case 11:
               //Right Tackle
               returnInt=2;
               break;
            case 12:
               //Tight End
               returnInt=3;
               break;
            case 13:
               //Wide Receiver
               returnInt=3;
               break;
            case 14:
               //Quarterback
               returnInt=3;
               break;
            case 15:
               //Running Back
               returnInt=4;
               break;
            case 17:
               //Defensive End
               returnInt=2;
               break;
            case 18:
               //Defensive Tackle
               returnInt=2;
               break;
            case 19:
               //Middle Linebacker
               returnInt=2;
               break;
            case 20:
               //Outside Linebacker
               returnInt=2;
               break;
            case 21:
               //Cornerback
               returnInt=3;
               break;
            case 26:
               //Free Safety
               returnInt=3;
               break;
            case 27:
               //Strong Safety
               returnInt=3;
               break;
            default:
			   Routines.writeToLog(servletName,"Position not found: PositionNumber=" + positionNumber,false,context);
	       }
      return returnInt;
      }

   static int[] getSkills(int[] skills,
                          int positionNumber,
                          int numOfSkills,
                          int selection,
	                      ServletContext context)
      {
	  String servletName="Routines.getSkills";	
      int[] returnInt=new int[5];
      if(selection==1)
        {
        for(int currentSkill=5;currentSkill<skills.length;currentSkill++)
           {
           if(skills[currentSkill]!=0)
             {
             returnInt[0]+=skills[currentSkill];
             }
           }
        if(positionNumber!=0)
          {
          returnInt[2]=(skills[0]+skills[1]+skills[2])/3;
          returnInt[1]=(skills[3]+skills[4])/2;
          if(numOfSkills!=0)
            {
            returnInt[0]=returnInt[0]/numOfSkills;
            }
          }
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnInt[0]=returnInt[0]/numOfSkills;
                 returnInt[1]=(skills[3]+skills[4])/2;
                 returnInt[2]=(skills[0]+skills[1]+skills[2])/3;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 1:
                 //Kicker
                 returnInt[3]=(skills[10]+
                               skills[11]+
                               skills[12]+
                               skills[13]+
                               skills[14]+
                               skills[15]+
                               skills[16]+
                               skills[17]+
                               skills[18]+
                               skills[19])/10;
                 returnInt[4]=(skills[7]+skills[8]+skills[9])/3;
	         break;
              case 2:
                 //Punter
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 7:
                 //Centre
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 8:
                 //Left Guard
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 9:
                 //Right Guard
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 10:
                 //Left Tackle
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 11:
                 //Right Tackle
                 returnInt[3]=skills[7];
                 returnInt[4]=skills[8];
	         break;
              case 12:
                 //Tight End
                 returnInt[3]=(skills[9]+skills[10]+skills[11]+skills[15])/4;
                 returnInt[4]=skills[8];
	         break;
              case 13:
                 //Wide Receiver
                 returnInt[3]=(skills[9]+skills[10]+skills[11]+skills[19])/4;
                 returnInt[4]=skills[8];
	         break;
              case 14:
                 //Quarterback
                 returnInt[3]=(skills[8]+skills[9]+skills[10])/3;
                 returnInt[4]=(skills[16]+skills[17]+skills[18]+skills[19])/4;
	         break;
              case 15:
                 //Running Back
                 returnInt[3]=(skills[9]+skills[10]+skills[11]+skills[12])/4;
                 returnInt[4]=(skills[13]+skills[14]+skills[15]+skills[20])/4;
	         break;
              case 17:
                 //Defensive End
                 returnInt[3]=skills[9];
                 returnInt[4]=skills[8];
	         break;
              case 18:
                 //Defensive Tackle
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[9];
	         break;
              case 19:
                 //Middle Linebacker
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[10];
	         break;
              case 20:
                 //Outside Linebacker
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[10];
	         break;
              case 21:
                 //Cornerback
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[10];
	         break;
              case 26:
                 //Free Safety
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[10];
	         break;
              case 27:
                 //Strong Safety
                 returnInt[3]=skills[8];
                 returnInt[4]=skills[10];
	         break;
	      default:
		        Routines.writeToLog(servletName,"Skills not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context); 
	       }
        }
      if(selection==2)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 1:
                 //Kicker
                 returnInt[0]=skills[10];
                 returnInt[1]=skills[11];
                 returnInt[2]=skills[12];
                 returnInt[3]=skills[13];
                 returnInt[4]=skills[14];
	         break;
              case 2:
                 //Punter
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[6];
                 returnInt[2]=skills[5];
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 7:
                 //Centre
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 8:
                 //Left Guard
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 9:
                 //Right Guard
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 10:
                 //Left Tackle
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 11:
                 //Right Tackle
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 12:
                 //Tight End
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[10];
                 returnInt[2]=skills[11];
				 returnInt[3]=skills[15];
                 returnInt[4]=(skills[12]+skills[13]+skills[14])/3;
                 break;
              case 13:
                 //Wide Receiver
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[10];
                 returnInt[2]=skills[11];
				 returnInt[3]=skills[19];
                 returnInt[4]=(skills[15]+skills[16])/2;
                 break;
              case 14:
                 //Quarterback
                 returnInt[0]=skills[8];
                 returnInt[1]=skills[9];
                 returnInt[2]=skills[10];
                 returnInt[3]=skills[11];
                 returnInt[4]=skills[12];
	         break;
              case 15:
                 //Running Back
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[10];
                 returnInt[2]=skills[11];
                 returnInt[3]=skills[12];
                 returnInt[4]=skills[13];
	         break;
              case 17:
                 //Defensive End
                 returnInt[0]=skills[10];
                 returnInt[1]=skills[7];
                 returnInt[2]=skills[6];
                 returnInt[3]=skills[5];
                 returnInt[4]=-1;
	         break;
              case 18:
                 //Defensive Tackle
                 returnInt[0]=skills[10];
                 returnInt[1]=skills[7];
                 returnInt[2]=skills[6];
                 returnInt[3]=skills[5];
                 returnInt[4]=-1;
	         break;
              case 19:
                 //Middle Linebacker
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[7];
                 returnInt[2]=(skills[11]+skills[12])/2;
                 returnInt[3]=skills[6];
                 returnInt[4]=skills[5];
	         break;
              case 20:
                 //Outside Linebacker
                 returnInt[0]=skills[9];
                 returnInt[1]=skills[7];
                 returnInt[2]=(skills[11]+skills[12])/2;
                 returnInt[3]=skills[6];
                 returnInt[4]=skills[5];
	         break;
              case 21:
                 //Cornerback
                 returnInt[0]=(skills[11]+skills[12])/2;
                 returnInt[1]=skills[9];
                 returnInt[2]=skills[7];
                 returnInt[3]=(skills[13]+skills[14])/2;
                 returnInt[4]=(skills[15]+skills[16])/2;
	         break;
              case 26:
                 //Free Safety
                 returnInt[0]=(skills[11]+skills[12])/2;
                 returnInt[1]=skills[9];
                 returnInt[2]=skills[7];
                 returnInt[3]=(skills[13]+skills[14])/2;
                 returnInt[4]=(skills[15]+skills[16])/2;
	         break;
              case 27:
                 //Strong Safety
                 returnInt[0]=(skills[11]+skills[12])/2;
                 returnInt[1]=skills[9];
                 returnInt[2]=skills[7];
                 returnInt[3]=(skills[13]+skills[14])/2;
                 returnInt[4]=(skills[15]+skills[16])/2;
	         break;
	      default:
		        Routines.writeToLog(servletName,"Skills not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context); 
	       }
        }
      if(selection==3)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 1:
                 //Kicker
                 returnInt[0]=(skills[15]+
                               skills[16]+
                               skills[17]+
                               skills[18]+
                               skills[19])/5;
                 returnInt[1]=skills[20];
                 returnInt[2]=skills[6];
                 returnInt[3]=skills[5];
                 returnInt[4]=-1;
	         break;
              case 2:
                 //Punter
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 7:
                 //Centre
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 8:
                 //Left Guard
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 9:
                 //Right Guard
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 10:
                 //Left Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 11:
                 //Right Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 12:
                 //Tight End
				 returnInt[0]=skills[7];
                 returnInt[1]=skills[6];
                 returnInt[2]=skills[5];
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 13:
                 //Wide Receiver
 				 returnInt[0]=(skills[17]+skills[18])/2;
                 returnInt[1]=(skills[12]+skills[13]+skills[14])/3;
                 returnInt[2]=skills[7];
                 returnInt[3]=skills[6];
                 returnInt[4]=skills[5];
	         break;
              case 14:
                 //Quarterback
                 returnInt[0]=skills[13];
                 returnInt[1]=(skills[14]+skills[15])/2;
                 returnInt[2]=skills[7];
                 returnInt[3]=skills[6];
                 returnInt[4]=skills[5];
	         break;
              case 15:
                 //Running Back
                 returnInt[0]=skills[14];
                 returnInt[1]=skills[15];
                 returnInt[2]=skills[20];
                 returnInt[3]=skills[8];
                 returnInt[4]=(skills[16]+skills[17])/2;
             break;
              case 17:
                 //Defensive End
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 18:
                 //Defensive Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 19:
                 //Middle Linebacker
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 20:
                 //Outside Linebacker
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 21:
                 //Cornerback
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 26:
                 //Free Safety
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 27:
                 //Strong Safety
                 returnInt[0]=skills[6];
                 returnInt[1]=skills[5];
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
	      default:
		        Routines.writeToLog(servletName,"Skills not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context); 
	       }
         }
      if(selection==4)
        {
        switch(positionNumber)
              {
              case 0:
                 //All Positions
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 1:
                 //Kicker
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 2:
                 //Punter
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 7:
                 //Centre
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 8:
                 //Left Guard
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 9:
                 //Right Guard
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 10:
                 //Left Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 11:
                 //Right Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 12:
                 //Tight End
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 13:
                 //Wide Receiver
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 14:
                 //Quarterback
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 15:
                 //Running Back
				 returnInt[0]=(skills[18]+skills[19])/2;
                 returnInt[1]=skills[7];
                 returnInt[2]=skills[6];
                 returnInt[3]=skills[5];
                 returnInt[4]=-1;
            break;
              case 17:
                 //Defensive End
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 18:
                 //Defensive Tackle
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 19:
                 //Middle Linebacker
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 20:
                 //Outside Linebacker
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 21:
                 //Cornerback
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 26:
                 //Free Safety
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
              case 27:
                 //Strong Safety
                 returnInt[0]=-1;
                 returnInt[1]=-1;
                 returnInt[2]=-1;
                 returnInt[3]=-1;
                 returnInt[4]=-1;
	         break;
	      default:
		        Routines.writeToLog(servletName,"Skills not set for: PositionNumber=" + positionNumber + ", Selection=" + selection,false,context); 
	       }
         }
      return returnInt;
      }

   static int[] getNumOfSkills(Connection database,
                               ServletContext context)
      {
	  String servletName="Routines.getNumOfSkills";	
      int maxPositionNumber=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionNumber " +
                                     "FROM positions " +
                                     "ORDER BY PositionNumber DESC");
        if(queryResult.first())
          {
          maxPositionNumber=queryResult.getInt(1)+1;
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);	
        }
      int[] positionSkills=new int[maxPositionNumber];
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionNumber,COUNT(PositionNumber) " +
                                     "FROM positionskills " +
                                     "GROUP BY PositionNumber");
        while(queryResult.next())
             {
             int positionNumber=queryResult.getInt(1);
             positionSkills[positionNumber]=queryResult.getInt(2);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);	
        }
      return positionSkills;
      }

   static int[][] sortPlayers(int positionNumber,
                              int[][] unSortedPlayers,
                              int numOfSkills[],
                              int sortPage,
                              int sortSkill,
                              Connection database,
	                          ServletContext context)
      {
	  String servletName="Routines.sortPlayers";	
      int returnInt[][]=new int[unSortedPlayers.length][27];
      String sortPlayers[]=new String[unSortedPlayers.length];
      for(int currentPlayer=0;currentPlayer<sortPlayers.length;currentPlayer++)
         {
         int tempPositionNumber=unSortedPlayers[currentPlayer][1];
         int[] tempSkills=new int[25];
         for(int currentSkill=0;currentSkill<tempSkills.length;currentSkill++)
            {
            tempSkills[currentSkill]=unSortedPlayers[currentPlayer][currentSkill+2];
            }
         int[] displaySkills=getSkills(tempSkills,positionNumber,numOfSkills[tempPositionNumber],sortPage,context);
         int primarySkill=((displaySkills[sortSkill-1]+5)/10)*10;
         switch((String.valueOf(primarySkill)).length())
               {
               case 0:
                  sortPlayers[currentPlayer]="000";
                  break;
               case 1:
                  sortPlayers[currentPlayer]="00"+primarySkill;
                  break;
               case 2:
                  sortPlayers[currentPlayer]="0"+primarySkill;
                  break;
               case 3:
                  sortPlayers[currentPlayer]=String.valueOf(primarySkill);
                  break;
               default:
			      Routines.writeToLog(servletName,"Unable to pad primaryskill",false,context); 
                  return returnInt;
               }
         int allSkills=0;
         for(int currentSkill=7;currentSkill<=26;currentSkill++)
            {
            allSkills+=unSortedPlayers[currentPlayer][currentSkill];
            }
         if(numOfSkills[tempPositionNumber]!=0)
           {   
           allSkills=((((allSkills)/numOfSkills[tempPositionNumber])+5)/10)*10;
           }
         switch((String.valueOf(allSkills)).length())
               {
               case 0:
                  sortPlayers[currentPlayer]+="0000";
                  break;
               case 1:
                  sortPlayers[currentPlayer]+="000"+allSkills;
                  break;
               case 2:
                  sortPlayers[currentPlayer]+="00"+allSkills;
                  break;
               case 3:
                  sortPlayers[currentPlayer]+="0"+allSkills;
                  break;
               case 4:
                  sortPlayers[currentPlayer]+=String.valueOf(allSkills);
                  break;
               default:
			      Routines.writeToLog(servletName,"Unable to pad skills",false,context); 
                  return returnInt;
               }
         int potential=(((((unSortedPlayers[currentPlayer][5]+unSortedPlayers[currentPlayer][6])/2)+5)/10)*10);
         switch((String.valueOf(potential)).length())
               {
               case 0:
                  sortPlayers[currentPlayer]+="000";
                  break;
       	       case 1:
                  sortPlayers[currentPlayer]+="00"+potential;
  	          break;
               case 2:
                  sortPlayers[currentPlayer]+="0"+potential;
  	          break;
               case 3:
                  sortPlayers[currentPlayer]+=String.valueOf(potential);
  	          break;
               default:
			      Routines.writeToLog(servletName,"Unable to pad potential",false,context);
                  return returnInt;
               }
         int personality=(((((unSortedPlayers[currentPlayer][2]+unSortedPlayers[currentPlayer][3]+unSortedPlayers[currentPlayer][4])/3)+5)/10)*10);
         switch((String.valueOf(personality)).length())
               {
               case 0:
                  sortPlayers[currentPlayer]+="000";
                  break;
       	       case 1:
                  sortPlayers[currentPlayer]+="00"+personality;
  	          break;
               case 2:
                  sortPlayers[currentPlayer]+="0"+personality;
  	          break;
               case 3:
                  sortPlayers[currentPlayer]+=String.valueOf(personality);
  	          break;
               default:
			      Routines.writeToLog(servletName,"Unable to pad personality",false,context);
                  return returnInt;
               }
         int playerNumber=unSortedPlayers[currentPlayer][0];
         switch(String.valueOf(playerNumber).length())
               {
               case 1:
                  sortPlayers[currentPlayer]+="0000000000"+playerNumber;
                  break;
               case 2:
                  sortPlayers[currentPlayer]+="000000000"+playerNumber;
                  break;
               case 3:
                  sortPlayers[currentPlayer]+="00000000"+playerNumber;
                  break;
               case 4:
                  sortPlayers[currentPlayer]+="0000000"+playerNumber;
                  break;
               case 5:
                  sortPlayers[currentPlayer]+="000000"+playerNumber;
                  break;
               case 6:
                  sortPlayers[currentPlayer]+="00000"+playerNumber;
                  break;
               case 7:
                  sortPlayers[currentPlayer]+="0000"+playerNumber;
                  break;
               case 8:
                  sortPlayers[currentPlayer]+="000"+playerNumber;
                  break;
               case 9:
                  sortPlayers[currentPlayer]+="00"+playerNumber;
                  break;
               case 10:
                  sortPlayers[currentPlayer]+="0"+playerNumber;
                  break;
               case 11:
                  sortPlayers[currentPlayer]+=playerNumber;
                  break;
               default:
			      Routines.writeToLog(servletName,"Unable to pad playerNumber",false,context);
                  return returnInt;
               }
           }
      Arrays.sort(sortPlayers);
      int sortedPlayer=0;
      int arrayLength=unSortedPlayers.length-1;
      for(int currentPlayer=arrayLength;currentPlayer>=0;currentPlayer--)
         {
         int playerNumber=Routines.safeParseInt(sortPlayers[currentPlayer].substring(13,24));
         for(int currentPlayer2=0;currentPlayer2<sortPlayers.length;currentPlayer2++)
            {
            if(playerNumber==unSortedPlayers[currentPlayer2][0])
              {
              returnInt[arrayLength-currentPlayer][0]=playerNumber;
              for(int currentSkill=1;currentSkill<27;currentSkill++)
                 {
                 returnInt[arrayLength-currentPlayer][currentSkill]=unSortedPlayers[currentPlayer2][currentSkill];
                 }
              }
            }
         }
      return returnInt;
      }

   static boolean[] playerDraft(int leagueNumber,
                                int teamNumber,
                                HttpSession session,
                                Connection database)
      {
      boolean[] returnBool=new boolean[2];
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT Season,Week,LockDown " +
                                      "FROM leagues " +
                                      "WHERE LeagueNumber=" + leagueNumber);
        queryResults.first();
        int season=queryResults.getInt(1);
        int week=queryResults.getInt(2);
        int lockDownValue=queryResults.getInt(3);
        if(season==1&&week==0)
          {
          queryResults=sql.executeQuery("SELECT Status " +
                                        "FROM teams " +
                                        "WHERE TeamNumber=" + teamNumber);
        queryResults.first();
          if(queryResults.getInt(1)==1)
            {
            returnBool[1]=true;
            }
          }
        if(lockDownValue==1)
          {
          returnBool[0]=true;
          }
        }
      catch(SQLException error)
        {
        session.setAttribute("message",error.getMessage());
        }
      return returnBool;
      }

   static String getDateTime(boolean nearestHour)
      {
      if(nearestHour)
        {
        DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd HH:00:00");
        return dbFormat.format(new Date());
        }
      else
        {
        DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dbFormat.format(new Date());
        }
      }

  static void turnCountDown(int leagueNumber,
                            boolean embedded,
                            boolean red,
                            Connection database,
                            PrintWriter webPageOutput,
                            ServletContext context,
                            HttpSession session)
      {
	  String servletName="Routines.turnCountDown";	
      int targetCCYY=0;
      int targetMM=0;
      int targetDD=0;
      int targetHH=0;
      int preSeasonWeeks=0;
      int regularSeasonWeeks=0;
      int postSeasonWeeks=0;
      int season=0;
      int week=0;
      DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd HH:00:00");
      int turnaround=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT DateTimeStamp,Alpha,PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,Season,Week " +
                                     "FROM leagues " +
                                     "WHERE LeagueNumber="+leagueNumber);
        while(queryResult.next())
             {
             String leagueDate=queryResult.getString(1);
             int alpha=queryResult.getInt(2);
             preSeasonWeeks=queryResult.getInt(3);
             regularSeasonWeeks=queryResult.getInt(4);
             postSeasonWeeks=queryResult.getInt(5);
             season=queryResult.getInt(6);
             week=queryResult.getInt(7);
             if(week==(preSeasonWeeks+regularSeasonWeeks+postSeasonWeeks))
               {
               week=0;
               season++;
               }
             turnaround=0;
             if(alpha==1)
               {
               turnaround=Routines.safeParseInt(context.getInitParameter("alphadays"));
               }
             else
               {
               turnaround=Routines.safeParseInt(context.getInitParameter("standarddays"));
               }
             targetCCYY=Routines.safeParseInt(leagueDate.substring(0,4));
             targetMM=Routines.safeParseInt(leagueDate.substring(5,7));
             targetDD=Routines.safeParseInt(leagueDate.substring(8,10));
             targetHH=Routines.safeParseInt(leagueDate.substring(11,13));
             }
          }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve league dates : " + error,false,context);	
        }
      java.util.Date targetDate;
      java.util.Date currentDate;
      long targetDateMil=0;
      long currentDateMil=0;
      Calendar targetCalendar=Calendar.getInstance();
      Calendar currentCalendar=Calendar.getInstance();
      currentCalendar.setTime(new java.util.Date());
      targetCalendar.set(Calendar.YEAR,targetCCYY);
      switch(targetMM)
        {
        case 1:
              targetCalendar.set(Calendar.MONTH,Calendar.JANUARY);
              break;
        case 2:
              targetCalendar.set(Calendar.MONTH,Calendar.FEBRUARY);
              break;
        case 3:
              targetCalendar.set(Calendar.MONTH,Calendar.MARCH);
              break;
        case 4:
              targetCalendar.set(Calendar.MONTH,Calendar.APRIL);
              break;
        case 5:
              targetCalendar.set(Calendar.MONTH,Calendar.MAY);
              break;
        case 6:
              targetCalendar.set(Calendar.MONTH,Calendar.JUNE);
              break;
        case 7:
              targetCalendar.set(Calendar.MONTH,Calendar.JULY);
              break;
        case 8:
              targetCalendar.set(Calendar.MONTH,Calendar.AUGUST);
              break;
        case 9:
              targetCalendar.set(Calendar.MONTH,Calendar.SEPTEMBER);
              break;
        case 10:
              targetCalendar.set(Calendar.MONTH,Calendar.OCTOBER);
              break;
        case 11:
              targetCalendar.set(Calendar.MONTH,Calendar.NOVEMBER);
              break;
         case 12:
              targetCalendar.set(Calendar.MONTH,Calendar.DECEMBER);
              break;
        default:
		      Routines.writeToLog(servletName,"Unable to find month",false,context); 
        }
      targetCalendar.set(Calendar.DATE,targetDD);
      targetCalendar.set(Calendar.HOUR_OF_DAY,targetHH);
      targetCalendar.set(Calendar.MINUTE,0);
      targetCalendar.set(Calendar.SECOND,0);
      targetCalendar.add(Calendar.DATE,turnaround);
      targetDate=targetCalendar.getTime();
      currentDate=currentCalendar.getTime();
      targetDateMil=targetDate.getTime();
      currentDateMil=currentDate.getTime();
      String currentDateT=dbFormat.format(currentCalendar.getTime());
      String targetDateT=dbFormat.format(targetCalendar.getTime());
      if(currentDateMil<targetDateMil)
        {
        if(embedded)
          {
          webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
          webPageOutput.println("<TH ALIGN=\"center\">");
          String countDownText1=Routines.decodeWeekNumber(preSeasonWeeks,
                                                          regularSeasonWeeks,
                                                          postSeasonWeeks,
                                                          season,
                                                          week,
                                                          false,
                                                          session);
          if(red)
            {
			webPageOutput.println("<FONT COLOR=\"#FF0000\">");                                                
            }
          webPageOutput.println("CountDown to "+countDownText1+"<BR>>>>");
          Routines.countDown(targetCalendar,webPageOutput);
          webPageOutput.println(Routines.indent(1));
          webPageOutput.println("<<<");
          if(red)
            {
			webPageOutput.println("</FONT>");
            }
		  webPageOutput.println("</TH>");
          webPageOutput.println("</TR>");
          }
        else
          {
          String countDownText1=Routines.decodeWeekNumber(preSeasonWeeks,
                                                          regularSeasonWeeks,
                                                          postSeasonWeeks,
                                                          season,
                                                          week,
                                                          false,
                                                          session);
          Routines.tableStart(false,webPageOutput);
          Routines.tableHeader("CountDown to "+countDownText1,0,webPageOutput);
          Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
		  if(red)
		    {
		    webPageOutput.println("<FONT COLOR=\"#FF0000\">");
		    }
          Routines.countDown(targetCalendar,webPageOutput);
		  if(red)
			{          
		    webPageOutput.println("</FONT>");
			}
          Routines.tableDataEnd(true,false,true,webPageOutput);
          Routines.tableEnd(webPageOutput);
          }
        }
      }
     static void advanceLeague(int leagueNumber,
                             ServletContext context,
                             Connection database)
         {
		 String servletName="Routines.advanceLeague";	
         DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd HH:00:00");
         int turnaround=0;
         int dateCCYY=0;
         int dateMM=0;
         int dateDD=0;
         int dateHH=0;
         int season=0;
         int week=0;
         int numOfWeeks=0;
         int status=0;
         boolean newSeason=false;
         try
           {
           Statement sql=database.createStatement();
           ResultSet queryResult;
           queryResult=sql.executeQuery("SELECT DateTimeStamp,Alpha,Season,Week,PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,Status " +
                                        "FROM leagues " +
                                        "WHERE LeagueNumber="+leagueNumber);
           queryResult.first();
           String leagueDate=queryResult.getString(1);
           int alpha=queryResult.getInt(2);
           season=queryResult.getInt(3);
		   week=queryResult.getInt(4);
		   numOfWeeks=queryResult.getInt(4)+queryResult.getInt(5)+queryResult.getInt(6);
		   status=queryResult.getInt(7);
//		   if(status==2)
//		     {
//		     week++;
//		     }
//		   if(week>numOfWeeks)
//		     {
//		     season++;
//		     week=0;	
//		     }
           turnaround=0;
           if(alpha==1)
             {
             turnaround=Routines.safeParseInt(context.getInitParameter("alphadays"));
             }
           else
             {
             turnaround=Routines.safeParseInt(context.getInitParameter("standarddays"));
             }
           dateCCYY=Routines.safeParseInt(leagueDate.substring(0,4));
           dateMM=Routines.safeParseInt(leagueDate.substring(5,7));
           dateDD=Routines.safeParseInt(leagueDate.substring(8,10));
           dateHH=Routines.safeParseInt(leagueDate.substring(11,13));
           }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve league date : " + error,false,context);	
        }
      Calendar calendar=Calendar.getInstance();
      calendar.set(Calendar.YEAR,dateCCYY);
      switch(dateMM)
        {
        case 1:
              calendar.set(Calendar.MONTH,Calendar.JANUARY);
              break;
        case 2:
              calendar.set(Calendar.MONTH,Calendar.FEBRUARY);
              break;
        case 3:
              calendar.set(Calendar.MONTH,Calendar.MARCH);
              break;
        case 4:
              calendar.set(Calendar.MONTH,Calendar.APRIL);
              break;
        case 5:
              calendar.set(Calendar.MONTH,Calendar.MAY);
              break;
        case 6:
              calendar.set(Calendar.MONTH,Calendar.JUNE);
              break;
        case 7:
              calendar.set(Calendar.MONTH,Calendar.JULY);
              break;
        case 8:
              calendar.set(Calendar.MONTH,Calendar.AUGUST);
              break;
        case 9:
              calendar.set(Calendar.MONTH,Calendar.SEPTEMBER);
              break;
        case 10:
              calendar.set(Calendar.MONTH,Calendar.OCTOBER);
              break;
        case 11:
              calendar.set(Calendar.MONTH,Calendar.NOVEMBER);
              break;
         case 12:
              calendar.set(Calendar.MONTH,Calendar.DECEMBER);
              break;
        default:
		      Routines.writeToLog(servletName,"Unable to find month",false,context);
        }
      calendar.set(Calendar.DATE,dateDD);
      calendar.set(Calendar.HOUR_OF_DAY,dateHH);
      calendar.set(Calendar.MINUTE,0);
      calendar.set(Calendar.SECOND,0);
      calendar.add(Calendar.DATE,turnaround);
      try
        {
        Statement sql=database.createStatement();
        int updated=sql.executeUpdate("UPDATE leagues " +
                                     "SET DateTimeStamp='" + dbFormat.format(calendar.getTime()) + "'," +
                                     "Status=2," +
                                     "Season=" + season + 
                                     ",Week=" + week +
                                     " WHERE LeagueNumber="+leagueNumber);
        if(updated!=1)
          {
		  Routines.writeToLog(servletName,"League date not advanced (" + leagueNumber + ")",false,context);	
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to update league : " + error,false,context);	
        }
	  Routines.updateGameBoard(leagueNumber,database,context);  
      }
	static int numOfTeams(int leagueNumber,
	                      ServletContext context,
						  Connection database)
	  {
	  String servletName="Routines.numOfTeams";	
	  int numOfTeams=0;
	  try
	    {
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT COUNT(TeamNumber) " +
									 "FROM leagueteams,divisions,conferences " +
									 "WHERE conferences.LeagueNumber=" + leagueNumber + " " +
									 "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
									 "AND leagueteams.DivisionNumber=divisions.DivisionNumber");
		if(queryResult.first())
		  {
		  numOfTeams=queryResult.getInt(1);
		  }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Error counting teams : " + error,false,context);
		}
	  return numOfTeams;	
   	  }      
      // Run Games - MOVE TO OWN SCREEN!!!
//      if (Routines.safeParseInt(request.getParameter("league"))!=0 && "runGames".equals(action))
//         {
//         GameEngine gameEngine = new GameEngine(Routines.safeParseInt(request.getParameter("league")),
//                                                database);
//         }
  }