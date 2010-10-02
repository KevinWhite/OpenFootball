import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Drafts extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Drafts";

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
	  String action=request.getParameter("action");  
	  if(action==null)
		{
		action="";
		}
	  int league=Routines.safeParseInt(request.getParameter("league"));
	  int view2=Routines.safeParseInt(request.getParameter("view2"));
	  int season=Routines.safeParseInt((String)session.getAttribute("viewSeason"));
	  if(season==0)
		{
		season=Routines.safeParseInt((String)session.getAttribute("season"));
		}
	  int view=Routines.safeParseInt(request.getParameter("view"));  
	  int round=0;
	  int team=0;
	  int position=0;
	  int college=0;
	  int letter=0;
	  String teamName="";
	  String positionName="";
	  String collegeName="";
	  String letterChar="";	
	  if(view==0&&action.equals("View"))
		{
		round=view2;
		}	    
	  if(view==1&&action.equals("View"))
		{
		team=view2;
		} 
	  if(view==2&&action.equals("View"))
	    {
		position=view2;
		} 
	  if(view==3&&action.equals("View"))
		{
		college=view2;
		} 
	  if(view==4&&action.equals("View"))
		{
		letter=view2;
		}		
	  String view2Text[]=new String[0];
	  int view2Values[]=new int[0];
	  int numOfTeams=Routines.numOfTeams(league,context,database);
	  int numOfRounds=0;				    
	  String style="";
	  if(view==1)
		{
		style="optsel";
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT teams.TeamNumber,Name " +
									   "FROM teams,leagueteams,divisions,conferences " +
									   "WHERE teams.TeamNumber=leagueteams.TeamNumber " +
									   "AND leagueteams.DivisionNumber=divisions.DivisionNumber " +
									   "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
									   "AND conferences.LeagueNumber=" + league + " " +
									   "ORDER BY Name ASC");
		  view2Text=new String[numOfTeams];
		  view2Values=new int[numOfTeams];
		  int currentValue=0;
		  while(queryResult.next())
			{
			if((team==0&&currentValue==0)||team==queryResult.getInt(1))
			  {
			  team=queryResult.getInt(1);
			  teamName=queryResult.getString(2);		
			  }
			view2Text[currentValue]=queryResult.getString(2);
			view2Values[currentValue]=queryResult.getInt(1);
			currentValue++;
			}
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Error accessing teams : " + error,false,context);
		  }
		}
	  else
		{
		style="opt";
		}
	  if(view==1)
		{
		session.setAttribute("viewTeam",String.valueOf(team));	
		}			  
      Routines.WriteHTMLHead("Drafts",//title
                             true,//showMenu
                             14,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     true,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
	  int myTeam=Routines.doIHaveAVerifiedTeamInThisLeague(league,database,session,request,context);
	  int[] positionSkills=Routines.getNumOfSkills(database,context);
	  // Prepare screen
      Routines.titleHeader("head2",true,webPageOutput);
      webPageOutput.println("<IMG SRC=\"../Images/Boss.gif\"" +
                            " WIDTH='160' HEIGHT='120' ALT='Boss'>");
      webPageOutput.println("<IMG SRC=\"../Images/Podium.gif\"" +
                            " WIDTH='90' HEIGHT='135' ALT='Commish'>");
      webPageOutput.println("<IMG SRC=\"../Images/MoneyMan.gif\"" +
                            " WIDTH='120' HEIGHT='120' ALT='MoneyMan'>");
      webPageOutput.println(Routines.spaceLines(1));
	  webPageOutput.println("Draft for Season " + season);
	  Routines.dotSpacer(webPageOutput);
      Routines.titleTrailer(true,webPageOutput);
      webPageOutput.println("<CENTER>");
      boolean liveDraft=false;
      boolean preDraft=false;
      boolean activeDraft=false;
      int currentRound=0;
      int currentPick=0;
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResults;
		queryResults=sql.executeQuery("SELECT Season,Week,LockDown,Status " +
									  "FROM leagues " +
									  "WHERE LeagueNumber=" + league);
		queryResults.first();
		int currentSeason=queryResults.getInt(1);
		int week=queryResults.getInt(2);
		int lockDown=queryResults.getInt(3);
		int leagueStatus=queryResults.getInt(4);
		int status=0;
		if(currentSeason==1&&week==0&&leagueStatus==1)
		  {
     	  preDraft=true;
		  }
		else
		  {
		  if(currentSeason>1&&week==0)
		    {
		    preDraft=true;	
		    if(lockDown==1)
		      {
		      activeDraft=true;	
		      }
		    }
		  liveDraft=true;  
		  }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Error accessing league : " + error,false,context);
		}
	  try
	    {
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT COUNT(PositionNumber) " +
							         "FROM positions " +
									 "WHERE RealPosition=1 " +
									 "AND Type!=3");
		numOfRounds=0;							 
		if(queryResult.first())
		  {
   		  numOfRounds=queryResult.getInt(1)*4;
		  } 
		}
	  catch(SQLException error)
	    {
	    Routines.writeToLog(servletName,"Error counting rounds : " + error,false,context);
	    }		   		  	
	  if(preDraft&&!activeDraft)
	    {
		Routines.turnCountDown(league,false,true,database,webPageOutput,context,session);
        webPageOutput.println(Routines.spaceLines(1));    		
	    }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT Round,Sequence " +
                                     "FROM draftboard " +
                                     "WHERE LeagueNumber= " + league);
        if(queryResult.first())
          {
          currentRound=queryResult.getInt(1);
          currentPick=queryResult.getInt(2);
          int order=0;
		  if(currentRound%2==0)
			{
			order=currentPick+1;
			}
		  else
			{
			order=numOfTeams-(currentPick);
			}
		  String selectingTeamName="";	
		  try
		    {
			queryResult=sql.executeQuery("SELECT Name " +
			  						     "FROM draftboardteam,teams " +
										 "WHERE LeagueNumber=" + league + " " +
										 "AND Sequence=" + order + " " +
										 "AND draftboardteam.TeamNumber=teams.TeamNumber");
			if(queryResult.first())
			  {
			  selectingTeamName=queryResult.getString(1);
			  }
			}
		  catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Error getting team : " + error,false,context);
			}		
		  if(activeDraft)
		    {		
            Routines.tableStart(false,webPageOutput);
            Routines.tableHeader("Draft In Progress",0,webPageOutput);
            Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
            webPageOutput.println("<FONT COLOR=\"#FF0000\">");
            webPageOutput.println("&nbsp*** " + "Round " + (currentRound+1) + ", Pick " + (currentPick+1) + ", " + selectingTeamName + " are now selecting *** ");
            webPageOutput.println("</FONT>");
            Routines.tableDataEnd(true,false,true,webPageOutput);
            Routines.tableEnd(webPageOutput);
		    webPageOutput.println(Routines.spaceLines(1));
		    }   
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Error accessing draftboard : " + error,false,context);
        }
      if(round==0)
	    {
	    round=currentRound;
	    }
	  if(round==0)
	    {
	    round=1;  
	    }
      if(view==0)
        {
        style="optsel";
        view2Text=new String[numOfRounds];
        view2Values=new int[numOfRounds];
        for(int currentValue=0;currentValue<view2Text.length;currentValue++)
           {
           view2Values[currentValue]=(currentValue+1);
           view2Text[currentValue]=String.valueOf(currentValue+1);
           }
        }
      else
        {
        style="opt";
        }
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Drafts",
                             "league=" +
                             league +
                             "&view=0",
                             "Round",
                             style,
                             true);
      webPageOutput.println("<B>·</B>");
      style="";
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Drafts",
                             "league=" +
                             league +
                             "&view=1",
                             "Team",
                             style,
                             true);
      webPageOutput.println("<B>·</B>");
      style="";
      if(view==2)
        {
        style="optsel";
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionNumber,PositionName " +
                                       "FROM positions " +
                                       "WHERE RealPosition=1 " +
                                       "AND Type!=3 " +
                                       "ORDER BY Type ASC, Sequence ASC");
          int numOfPositions=0;
          while(queryResult.next())
             {
             numOfPositions++;
             }
		  int currentValue=0;
		  if(preDraft||activeDraft)
		    {   
		    view2Text=new String[numOfPositions+1];
            view2Values=new int[numOfPositions+1];
            view2Text[0]="All Positions";
		    positionName="All Positions";
            view2Values[0]=-1;
            queryResult.beforeFirst();
            currentValue=1;
		    }
		  else
		    {  
			view2Text=new String[numOfPositions];
			view2Values=new int[numOfPositions];
			queryResult.beforeFirst();
		    }
          while(queryResult.next())
             {
			 if(((preDraft||activeDraft)&&position==0&&currentValue==1)
  			      ||((!preDraft&&!activeDraft)&&position==0&&currentValue==0)
			      ||position==queryResult.getInt(1))
			   {
			   if(((preDraft||activeDraft)&&position==0&&currentValue==1)
			     ||((!preDraft&&!activeDraft)&&position==0&&currentValue==0))
			     {
			     view2=queryResult.getInt(1);		
			     }
			   position=queryResult.getInt(1);
			   positionName=queryResult.getString(2);		
			   }             	
             view2Text[currentValue]=queryResult.getString(2);
             view2Values[currentValue]=queryResult.getInt(1);
             currentValue++;
             }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Error getting positions : " + error,false,context);
          }
        }
      else
        {
        style="opt";
        }
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Drafts",
                             "league=" +
                             league +
                             "&view=2",
                             "Position",
                             style,
                             true);
      webPageOutput.println("<B>·</B>");
      style="";
      if(view==3)
        {
        style="optsel";
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT CollegeNumber,CollegeName " +
                                       "FROM colleges " +
                                       "ORDER BY CollegeName ASC");
          int numOfColleges=0;
          while(queryResult.next())
            {
            numOfColleges++;
            }
          view2Text=new String[numOfColleges];
          view2Values=new int[numOfColleges];
          queryResult.beforeFirst();
          int currentValue=0;
          while(queryResult.next())
            {
			if((college==0&&currentValue==0)||college==queryResult.getInt(1))
			  {
			  college=queryResult.getInt(1);
			  collegeName=queryResult.getString(2);		
			  }            	
            view2Text[currentValue]=queryResult.getString(2);
            view2Values[currentValue]=queryResult.getInt(1);
            currentValue++;
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Error counting positions 2 : " + error,false,context);
          }
        }
      else
        {
        style="opt";
        }
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Drafts",
                             "league=" +
                             league +
                             "&view=3",
                             "College",
                             style,
                             true);
      webPageOutput.println("<B>·</B>");
      style="";
      if(view==4)
        {
        style="optsel";
        view2Text=new String[26];
        view2Values=new int[26];
        String loadValues[]={"A","B","C","D","E","F","G","H","I","J","K","L","M",
                             "N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        for(int currentItem=0;currentItem<26;currentItem++)
           {
		   if((letter==0&&currentItem==0)||letter==currentItem)
			 {
			 letter=currentItem;
			 letterChar=loadValues[currentItem];		
			 }            	
           view2Values[currentItem]=currentItem;
           view2Text[currentItem]=loadValues[currentItem];
           }
        }
      else
        {
        style="opt";
        }
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "Drafts",
                             "league=" +
                             league +
                             "&view=4",
                             "Alphabetical",
                             style,
                             true);
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Drafts\" METHOD=\"POST\">");
      if(view==0)
        {
        webPageOutput.println("<B>ROUNDS: </B>");
        }
      if(view==1)
        {
        webPageOutput.println("<B>TEAMS: </B>");
        }
      if(view==2)
        {
        webPageOutput.println("<B>POSITION: </B>");
        }
      if(view==3)
        {
        webPageOutput.println("<B>COLLEGE: </B>");
        }
      if(view==4)
        {
        webPageOutput.println("<B>ALPHABETICAL: </B>");
        }
      webPageOutput.println("<SELECT NAME=\"view2\">");
      String selected="";
      for(int currentView=0;currentView<view2Text.length;currentView++)
         {
         if(view2Values[currentView]==view2)
           {
           selected=" SELECTED";
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + view2Values[currentView] + "\">" + view2Text[currentView]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      //By Round View
      if(view==0)
        {
        int viewRound=round;
        webPageOutput.println(Routines.spaceLines(2));
        webPageOutput.println("<DIV CLASS=\"SLTables2\">");
        webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
        webPageOutput.println("<TBODY>");
        webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
        webPageOutput.println("<TD CLASS=\"bg0\" COLSPAN=\"8\"><FONT CLASS=\"bg0font\">ROUND "+viewRound+"</FONT></TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("<TR ALIGN=\"center\" class=\"bg4\">");
        webPageOutput.println("   <TD ALIGN=\"left\">Pick</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">Overall</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">Team</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">Player</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">Position</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">College</TD>");
        webPageOutput.println("   <TD ALIGN=\"left\">Rating</TD>");
        webPageOutput.println("</TR>");

        try
          {
          Statement sql1=database.createStatement();
          ResultSet queryResult1;
          Statement sql2=database.createStatement();
          ResultSet queryResult2=null;
          String order="";
          if(viewRound%2==0)
            {
            order="DESC";
            }
          else
            {
            order="ASC";
            }  
          if(preDraft||activeDraft)
            {  
            queryResult1=sql1.executeQuery("SELECT teams.TeamNumber,Name " +
                                           "FROM teams,draftboardteam " +
                                           "WHERE LeagueNumber=" + league + " " +
                                           "AND draftboardteam.TeamNumber=teams.TeamNumber " +
                                           "ORDER BY Sequence " + order);
            queryResult2=sql2.executeQuery("SELECT PlayerNumber,Forname,Surname,positions.PositionNumber,PositionName,DraftedTeam,CollegeName,colleges.CollegeNumber," +
			                               "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
										   "Skill1,Skill2,Skill3,Skill4,Skill5," +
										   "Skill6,Skill7,Skill8,Skill9,Skill10," +
										   "Skill11,Skill12,Skill13,Skill14,Skill15," +
										   "Skill16,Skill17,Skill18,Skill19,Skill20 " +
                                           "FROM players,positions,colleges " +
				                           "WHERE WorldNumber=" + league + " " +
                                           "AND DraftedRound=" + round + " " +
                                           "AND players.PositionNumber=positions.PositionNumber " +
                                           "AND players.CollegeNumber=colleges.CollegeNumber " +
                                           "ORDER BY DraftedPick ASC");
            }
          else
            {
    		queryResult1=sql1.executeQuery("SELECT teams.TeamNumber,teams.Name,PlayerNumber,Forname,Surname,positions.PositionNumber,PositionName,DraftedTeam,CollegeName,colleges.CollegeNumber," +
			                               "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
										   "Skill1,Skill2,Skill3,Skill4,Skill5," +
										   "Skill6,Skill7,Skill8,Skill9,Skill10," +
										   "Skill11,Skill12,Skill13,Skill14,Skill15," +
										   "Skill16,Skill17,Skill18,Skill19,Skill20 " +
										   "FROM players,positions,colleges,teams " +
       				                       "WHERE DraftedRound=" + round + " " +
										   "AND players.WorldNumber=" + league + " " +
										   "AND players.PositionNumber=positions.PositionNumber " +
										   "AND players.CollegeNumber=colleges.CollegeNumber " +
										   "AND players.TeamNumber=teams.TeamNumber " +
										   "ORDER BY DraftedPick ASC");            	                                 
            }
          int sequence=1;
          while(queryResult1.next())
               {
               int teamNumber=queryResult1.getInt(1);
               teamName=queryResult1.getString(2);
			   int playerNumber=0;
			   String name="";
			   int positionNumber=0;
			   positionName="";
			   int overallSequence=0;
			   int draftedTeam=0;
			   collegeName="";
			   int collegeNumber=0;
			   int skills[]=new int[25];
               if(preDraft||activeDraft)
                 {
  			     if(queryResult2.next())
				   {
				   playerNumber=queryResult2.getInt(1);
				   name=queryResult2.getString(3) + "," + queryResult2.getString(2);
				   positionNumber=queryResult2.getInt(4);
				   positionName=queryResult2.getString(5);
				   draftedTeam=queryResult2.getInt(6);
				   collegeName=queryResult2.getString(7);
				   collegeNumber=queryResult2.getInt(8);
				   for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					  {
					  skills[currentSkill]=queryResult2.getInt(9+currentSkill);
					  }
				   if(playerNumber!=0&&teamNumber!=draftedTeam)
					 {
					 Routines.writeToLog(servletName,"Drafted player mismatch (calculatedTeam="+teamNumber+",foundTeam="+draftedTeam,false,context);
					 }
				   }                		
                 }
              else
                 {
    			 playerNumber=queryResult1.getInt(3);
				 name=queryResult1.getString(5) + "," + queryResult1.getString(4);
				 positionNumber=queryResult1.getInt(6);
				 positionName=queryResult1.getString(7);
				 draftedTeam=queryResult1.getInt(8);
				 collegeName=queryResult1.getString(9);
				 collegeNumber=queryResult1.getInt(10);
				 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					{
					skills[currentSkill]=queryResult1.getInt(11+currentSkill);
					}	   
                 }
			  int totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);
              overallSequence=((round-1)*numOfTeams)+sequence;
              webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
              webPageOutput.println("   <TD ALIGN=\"left\">"+sequence+"</TD>");
              webPageOutput.println("   <TD ALIGN=\"left\">"+overallSequence+"</TD>");
              webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
			  Routines.WriteHTMLLink(request,
									 response,
									 webPageOutput,
									 "Drafts",
									 "league=" +
									 league +
									 "&view=1" +
									 "&view2=" + 
									 teamNumber +
									 "&team=" +
									 teamNumber +
									 "&action=View",
									 queryResult1.getString(2),
									 "bg2",
									 true);
			  webPageOutput.println("    </NOBR></TD>");						 
              if(playerNumber==0)
                {
                webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
                webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
                webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
                webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
                }
              else
                {
                webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Players",
									   "league=" +
									   league +
									   "&view=1" +
									   "&view2=" + 
									   playerNumber +
									   "&action=View",
									   name,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
                webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Drafts",
									   "league=" +
									   league +
									   "&view=2" +
									   "&view2=" + 
									   positionNumber +
									   "&action=View",
									   positionName,
									   "bg2",
									   true);
				webPageOutput.println("   </NOBR></TD>");
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Drafts",
									   "league=" +
									   league +
									   "&view=3" +
									   "&view2=" + 
									   collegeNumber +
									   "&action=View",
									   collegeName,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
                webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
                }
              webPageOutput.println("</TR>");
              sequence++;
              }
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error getting Round order : " + error,false,context);
            }
        webPageOutput.println("</TBODY>");
        webPageOutput.println("</TABLE>");
        webPageOutput.println("</DIV>");
        }
        //By Team View
		if(view==1)
		  {
		  int viewTeam=team;
		  webPageOutput.println(Routines.spaceLines(2));
		  webPageOutput.println("<DIV CLASS=\"SLTables2\">");
		  webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
		  webPageOutput.println("<TBODY>");
		  webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
		  webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"8\">");
		  webPageOutput.println("<FONT CLASS=\"home\">");
		  webPageOutput.println(teamName);
		  webPageOutput.println("</FONT>");
		  webPageOutput.println("</TD>");		  
		  webPageOutput.println("</TR>");
		  webPageOutput.println("<TR ALIGN=\"center\" class=\"bg4\">");
		  webPageOutput.println("   <TD ALIGN=\"left\">Round</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Pick</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Overall</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Player</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Position</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">College</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Rating</TD>");
		  webPageOutput.println("</TR>");

		  try
			{
			Statement sql=database.createStatement();
			ResultSet queryResult;
			int sequence=0;
			if(preDraft||activeDraft)
			  {  
			  queryResult=sql.executeQuery("SELECT Sequence " +
						  			       "FROM teams,draftboardteam " +
										   "WHERE LeagueNumber=" + league + " " +
										   "AND draftboardteam.TeamNumber=teams.TeamNumber " +
										   "AND draftboardteam.TeamNumber=" + team);
			  if(queryResult.first())
			    {
			    sequence=queryResult.getInt(1);							 
			    }
			  int printSequence=0;  
   		      for(currentRound=0;currentRound<numOfRounds;currentRound++)
			     {			  
                 queryResult=sql.executeQuery("SELECT PlayerNumber,Forname,Surname,positions.PositionNumber,PositionName,CollegeName,colleges.CollegeNumber," +
				                              "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                              "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                              "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                              "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                              "Skill16,Skill17,Skill18,Skill19,Skill20 " +
		    	    					      "FROM players,positions,colleges " +
										      "WHERE TeamNumber=" + team + " " +
										      "AND DraftedRound=" + (currentRound+1) + " " +
										      "AND players.PositionNumber=positions.PositionNumber " +
										      "AND players.CollegeNumber=colleges.CollegeNumber " + 
										      "AND Type!=3");
 				 int pick=0;
				 if(currentRound%2!=0)
				   {
				   pick=(currentRound*numOfTeams)+((numOfTeams-sequence)+1);
				   printSequence=(numOfTeams-sequence)+1;
				   }
				 else
				   {
				   pick=(currentRound*numOfTeams)+sequence;	
				   printSequence=sequence;
				   }
				 int playerNumber=0;
				 String name="";
				 int positionNumber=0;
				 positionName="";
				 collegeName="";
				 int collegeNumber=0;
				 int totalSkills=0;				   
				 if(queryResult.first())
				   {	
				   playerNumber=queryResult.getInt(1);
				   name=queryResult.getString(3) + "," + queryResult.getString(2);
				   positionNumber=queryResult.getInt(4);
				   positionName=queryResult.getString(5);
				   collegeName=queryResult.getString(6);
				   collegeNumber=queryResult.getInt(7);
				   int skills[]=new int[25];
				   for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					  {
					  skills[currentSkill]=queryResult.getInt(8+currentSkill);
					  }
				   totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);
				   }	
				webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Drafts",
									   "league=" +
									   league +
									   "&view=0" +
									   "&view2=" + 
									   (currentRound+1) +
									   "&action=View",
									   String.valueOf(currentRound+1),
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
				webPageOutput.println("   <TD ALIGN=\"left\">"+printSequence+"</TD>");
				webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				if(playerNumber==0)
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  }
				else
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Players",
										 "league=" +
										 league +
										 "&view=3" +
										 "&view2=" + 
										 playerNumber +
										 "&action=View",
										 name,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=2" +
										 "&view2=" + 
										 positionNumber +
										 "&action=View",
										 positionName,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=3" +
										 "&view2=" + 
										 collegeNumber +
										 "&action=View",
										 collegeName,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
				  }
				webPageOutput.println("</TR>");
				}
			   }
			  else
			   {
				{  
			   queryResult=sql.executeQuery("SELECT DraftedRound,DraftedPick,PlayerNumber,Forname,Surname,positions.PositionNumber,PositionName,CollegeName,colleges.CollegeNumber," +
			                                "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
			                                "Skill1,Skill2,Skill3,Skill4,Skill5," +
			                                "Skill6,Skill7,Skill8,Skill9,Skill10," +
			                                "Skill11,Skill12,Skill13,Skill14,Skill15," +
			                                "Skill16,Skill17,Skill18,Skill19,Skill20 " +
											"FROM players,positions,colleges " +
											"WHERE TeamNumber=" + team + " " +
											"AND players.PositionNumber=positions.PositionNumber " +
											"AND players.CollegeNumber=colleges.CollegeNumber " +
					                        "AND Type!=3 " +
											"ORDER BY DraftedRound ASC, DraftedPick ASC");
			   while(queryResult.next())
			      {		
			      round=queryResult.getInt(1);
			      int pick=queryResult.getInt(2);	
				  int playerNumber=queryResult.getInt(3);
				  String name=queryResult.getString(5) + "," + queryResult.getString(4);
				  int positionNumber=queryResult.getInt(6);
				  positionName=queryResult.getString(7);
				  collegeName=queryResult.getString(8);
				  int collegeNumber=queryResult.getInt(9);
				  int skills[]=new int[25];
				  for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					 {
					 skills[currentSkill]=queryResult.getInt(10+currentSkill);
					 }	
				  int totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);	 		      							
  			      int overallPick=((round-1)*numOfTeams)+pick;
				  webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=0" +
										 "&view2=" + 
										 round +
										 "&action=View",
										 String.valueOf(round),
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");
				  if(playerNumber==0)
					{
					webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					}
				  else
					{
					webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
					Routines.WriteHTMLLink(request,
										   response,
										   webPageOutput,
										   "Players",
										   "league=" +
										   league +
										   "&view=3" +
										   "&view2=" + 
										   playerNumber +
										   "&action=View",
										   name,
										   "bg2",
										   true);
					webPageOutput.println("    </NOBR></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
					Routines.WriteHTMLLink(request,
										   response,
										   webPageOutput,
										   "Drafts",
										   "league=" +
										   league +
										   "&view=2" +
										   "&view2=" + 
										   positionNumber +
										   "&action=View",
										   positionName,
										   "bg2",
										   true);
					webPageOutput.println("    </NOBR></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
					Routines.WriteHTMLLink(request,
										   response,
										   webPageOutput,
										   "Drafts",
										   "league=" +
										   league +
										   "&view=3" +
										   "&view2=" + 
										   collegeNumber +
										   "&action=View",
										   collegeName,
										   "bg2",
										   true);
					webPageOutput.println("    </NOBR></TD>");
					webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
					}
				  webPageOutput.println("</TR>");
				  }
				 }				   	 	
			   }
			  }
			catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Error getting Team order : " + error,false,context);
			  }
			}  
		//By Position View
		if(view==2)
		  {
		  int viewPosition=position;
		  webPageOutput.println(Routines.spaceLines(2));
		  webPageOutput.println("<DIV CLASS=\"SLTables2\">");
		  webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
		  webPageOutput.println("<TBODY>");
		  webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
		  webPageOutput.println("<TD CLASS=\"bg0\" COLSPAN=\"8\"><FONT CLASS=\"bg0font\">"+positionName+"</FONT></TD>");
		  webPageOutput.println("</TR>");
		  webPageOutput.println("<TR ALIGN=\"center\" class=\"bg4\">");
		  webPageOutput.println("   <TD ALIGN=\"left\">Round</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Pick</TD>");
  		  webPageOutput.println("   <TD ALIGN=\"left\">Overall</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Team</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Player</TD>");
		  if(viewPosition==-1)
		    {
			webPageOutput.println("   <TD ALIGN=\"left\">Position</TD>");	
		    }
		  webPageOutput.println("   <TD ALIGN=\"left\">College</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Rating</TD>");
		  webPageOutput.println("</TR>");
          try
		  	{
		  	Statement sql1=database.createStatement();
			ResultSet queryResult1;
			Statement sql2=database.createStatement();
			ResultSet queryResult2;			
			int sequence=0;
			if(preDraft||activeDraft)
			  {
			  if(viewPosition==-1)
			    {  
				queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,CollegeName,colleges.CollegeNumber,players.TeamNumber,DraftedRound,DraftedPick,PositionName,players.PositionNumber," +
				                               "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                               "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                               "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                               "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                               "Skill16,Skill17,Skill18,Skill19,Skill20 " +
											   "FROM players,draftratings,colleges,positions " +
											   "WHERE players.PlayerNumber=draftratings.PlayerNumber " +
											   "AND draftratings.TeamNumber=" + myTeam + " " +
											   "AND players.PositionNumber=positions.PositionNumber " +
											   "AND players.Experience=0 " +
											   "AND players.CollegeNumber=colleges.CollegeNumber " +
											   "ORDER BY OverallRating ASC");			    	
			    }
			  else
			    {  
			    queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,CollegeName,colleges.CollegeNumber,players.TeamNumber,DraftedRound,DraftedPick," +
				                               "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                               "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                               "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                               "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                               "Skill16,Skill17,Skill18,Skill19,Skill20 " +
				  				  		       "FROM players,draftratings,colleges " +
										       "WHERE players.PlayerNumber=draftratings.PlayerNumber " +
										       "AND draftratings.TeamNumber=" + myTeam + " " +
										       "AND players.PositionNumber = " + viewPosition + " " +
										       "AND players.Experience=0 " +
										       "AND players.CollegeNumber=colleges.CollegeNumber " +
										       "ORDER BY PositionRating ASC");
			    }						     
			  int playerNumber=0;
			  String playerName="";
			  collegeName="";
			  positionName="";
			  int collegeNumber=0;
			  int teamNumber=0;
			  int positionNumber=0;
			  teamName="";							     			  
			  int overallPick=0;
			  round=0;
			  int pick=0;	
			  while(queryResult1.next())
				{
				playerNumber=queryResult1.getInt(1);
				playerName=queryResult1.getString(3) + "," + queryResult1.getString(2);
				collegeName=queryResult1.getString(4);
				collegeNumber=queryResult1.getInt(5);
				teamNumber=queryResult1.getInt(6);	
				round=queryResult1.getInt(7);
				pick=queryResult1.getInt(8);
				int skills[]=new int[25];
				if(viewPosition==-1)
				  {
				  positionName=queryResult1.getString(9);
				  positionNumber=queryResult1.getInt(10);
				  for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					 {
					 skills[currentSkill]=queryResult1.getInt(11+currentSkill);
					 }				  
				  }
				else
				  {
				  for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					 {
					 skills[currentSkill]=queryResult1.getInt(9+currentSkill);
					 }	  
				  }
				int totalSkills=0;
				if(viewPosition==-1)
				  {
				  totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);	
				  }
				else
				  { 	
				  totalSkills = Routines.getSkillRating((positionSkills[viewPosition]),skills);
				  }  
				teamName="";
				overallPick=0;
				if(teamNumber!=0)
				  {
				  overallPick=((round-1)*numOfTeams)+pick;
				  queryResult2=sql2.executeQuery("SELECT Name " +
				  							     "FROM teams " +
											     "WHERE TeamNumber="+teamNumber);
				  if(queryResult2.first())
				    {
				    teamName=queryResult2.getString(1);							     
				    }
				  }						     						 
				webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				if(teamNumber==0)
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");	
				  }
				else
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=0" +
										 "&view2=" + 
										 round +
										 "&action=View",
										 String.valueOf(round),
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");				  	
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=1" +
										 "&view2=" + 
										 teamNumber +
					                     "&team=" +
										 teamNumber +
										 "&action=View",
										 teamName,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");	  
				  }
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Players",
									   "league=" +
									   league +
									   "&view=3" +
									   "&view2=" + 
									   playerNumber +
									   "&action=View",
									   playerName,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
				if(viewPosition==-1)
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=2" +
										 "&view2=" + 
										 positionNumber +
										 "&action=View",
										 positionName,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  }
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Drafts",
									   "league=" +
									   league +
									   "&view=3" +
									   "&view2=" + 
									   collegeNumber +
									   "&action=View",
									   collegeName,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
				webPageOutput.println("</TR>");
				}
			  }
			else
			{
			for(int currentLoop=0;currentLoop<2;currentLoop++)
			   {
			   if(currentLoop==0)
			     {	
		         queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,CollegeName,colleges.CollegeNumber,players.TeamNumber,DraftedRound,DraftedPick," +
				                                "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                                "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                                "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                                "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                                "Skill16,Skill17,Skill18,Skill19,Skill20 " +
				   						        "FROM players,colleges " +
										        "WHERE players.PositionNumber = " + viewPosition + " " +
										        "AND players.Experience=0 " +
										        "AND players.CollegeNumber=colleges.CollegeNumber " +
										        "AND players.WorldNumber=" + league + " " +
										        "AND players.TeamNumber!=0 " +
										        "ORDER BY DraftedRound ASC, DraftedPick ASC");
			     }
			   else
			     {  	
				 queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,CollegeName,colleges.CollegeNumber,players.TeamNumber,DraftedRound,DraftedPick," +
				                                "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                                "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                                "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                                "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                                "Skill16,Skill17,Skill18,Skill19,Skill20 " +
				 							    "FROM players,colleges " +
												"WHERE players.PositionNumber = " + viewPosition + " " +
												"AND players.Experience=0 " +
												"AND players.CollegeNumber=colleges.CollegeNumber " +
					                            "AND players.WorldNumber=" + league + " " +
												"AND players.TeamNumber=0 " +												
												"ORDER BY Surname ASC, Forname ASC");			     										        
			     }
    		   int playerNumber=0;
			   String playerName="";
			   collegeName="";
			   int collegeNumber=0;
			   int teamNumber=0;
			   teamName="";							     			  
			   int overallPick=0;
			   round=0;
			   int pick=0;	
			   while(queryResult1.next())
			     {
			     playerNumber=queryResult1.getInt(1);
			     playerName=queryResult1.getString(3) + "," + queryResult1.getString(2);
			     collegeName=queryResult1.getString(4);
			     collegeNumber=queryResult1.getInt(5);
			     teamNumber=queryResult1.getInt(6);	
			     round=queryResult1.getInt(7);
			     pick=queryResult1.getInt(8);
				 int skills[]=new int[25];
				 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					{
					skills[currentSkill]=queryResult1.getInt(9+currentSkill);
					}
				 int totalSkills = Routines.getSkillRating((positionSkills[position]),skills);	
                 teamName="";
			     overallPick=0;
			     if(teamNumber!=0)
				   {
				   overallPick=((round-1)*numOfTeams)+pick;
				   queryResult2=sql2.executeQuery("SELECT Name " +
					   						      "FROM teams " +
											      "WHERE TeamNumber="+teamNumber);
				   if(queryResult2.first())
				     {
				     teamName=queryResult2.getString(1);							     
				     }
				   }						     						 
			     webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				 if(teamNumber==0)
				   {
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");	
				   }
				 else
				   {
				   webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				   Routines.WriteHTMLLink(request,
										  response,
										  webPageOutput,
										  "Drafts",
										  "league=" +
										  league +
										  "&view=0" +
										  "&view2=" + 
										  round +
										  "&action=View",
										  String.valueOf(round),
										  "bg2",
										  true);
				   webPageOutput.println("    </NOBR></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");				  	
				   webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				   Routines.WriteHTMLLink(request,
										  response,
										  webPageOutput,
										  "Drafts",
										  "league=" +
										  league +
										  "&view=1" +
										  "&view2=" + 
										  teamNumber +
					                      "&team=" +
										  teamNumber +
										  "&action=View",
										  teamName,
										  "bg2",
										  true);
				   webPageOutput.println("    </NOBR></TD>");
				   }
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Players",
										"league=" +
										league +
										"&view=3" +
										"&view2=" + 
										playerNumber +
										"&action=View",
										playerName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Drafts",
										"league=" +
										league +
										"&view=3" +
										"&view2=" + 
										collegeNumber +
										"&action=View",
										collegeName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
			     webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
			     webPageOutput.println("</TR>");
			     }
			   }   
			}			
 		  }
		  catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Error getting Position order : " + error,false,context);
			  }
			}  			
		//By College View
		if(view==3)
		  {
		  webPageOutput.println(Routines.spaceLines(2));
		  webPageOutput.println("<DIV CLASS=\"SLTables2\">");
		  webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
		  webPageOutput.println("<TBODY>");
		  webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
		  webPageOutput.println("<TD CLASS=\"bg0\" COLSPAN=\"8\"><FONT CLASS=\"bg0font\">"+collegeName+"</FONT></TD>");
		  webPageOutput.println("</TR>");
		  webPageOutput.println("<TR ALIGN=\"center\" class=\"bg4\">");
		  webPageOutput.println("   <TD ALIGN=\"left\">Round</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Pick</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Overall</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Team</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Player</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Position</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Rating</TD>");
		  webPageOutput.println("</TR>");
		  try
			{
			Statement sql1=database.createStatement();
			ResultSet queryResult1;
			Statement sql2=database.createStatement();
			ResultSet queryResult2;			
			int sequence=0;
			if(preDraft||activeDraft)
			  {
			  queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,PositionName,positions.PositionNumber,players.TeamNumber,DraftedRound,DraftedPick," +
			                                 "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
			                                 "Skill1,Skill2,Skill3,Skill4,Skill5," +
			                                 "Skill6,Skill7,Skill8,Skill9,Skill10," +
			                                 "Skill11,Skill12,Skill13,Skill14,Skill15," +
			                                 "Skill16,Skill17,Skill18,Skill19,Skill20 " +
											 "FROM players,draftratings,positions " +
											 "WHERE players.PlayerNumber=draftratings.PlayerNumber " +
											 "AND draftratings.TeamNumber=" + myTeam + " " +
											 "AND players.CollegeNumber = " + college + " " +
											 "AND players.Experience=0 " +
											 "AND players.PositionNumber=positions.PositionNumber " +
				                             "AND Type!=3 " +
											 "ORDER BY PositionRating ASC");
			  int playerNumber=0;
			  String playerName="";
			  positionName="";
			  int positionNumber=0;
			  int teamNumber=0;
			  teamName="";							     			  
			  int overallPick=0;
			  round=0;
			  int pick=0;	
			  while(queryResult1.next())
				{
				playerNumber=queryResult1.getInt(1);
				playerName=queryResult1.getString(3) + "," + queryResult1.getString(2);
				positionName=queryResult1.getString(4);
				positionNumber=queryResult1.getInt(5);
				teamNumber=queryResult1.getInt(6);	
				round=queryResult1.getInt(7);
				pick=queryResult1.getInt(8);
				int skills[]=new int[25];
				for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
				   {
				   skills[currentSkill]=queryResult1.getInt(9+currentSkill);
				   }
				int totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);
				teamName="";
				overallPick=0;
				if(teamNumber!=0)
				  {
				  overallPick=((round-1)*numOfTeams)+pick;
				  queryResult2=sql2.executeQuery("SELECT Name " +
												 "FROM teams " +
												 "WHERE TeamNumber="+teamNumber);
				  if(queryResult2.first())
					{
					teamName=queryResult2.getString(1);							     
					}
				  }						     						 
				webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				if(teamNumber==0)
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\"></TD>");	
				  }
				else
				  {
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=0" +
										 "&view2=" + 
										 round +
										 "&action=View",
										 String.valueOf(round),
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				  webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");				  	
				  webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				  Routines.WriteHTMLLink(request,
										 response,
										 webPageOutput,
										 "Drafts",
										 "league=" +
										 league +
										 "&view=1" +
										 "&view2=" + 
										 teamNumber +
  					                     "&team=" +
										 teamNumber +
										 "&action=View",
										 teamName,
										 "bg2",
										 true);
				  webPageOutput.println("    </NOBR></TD>");
				  }
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Players",
									   "league=" +
									   league +
									   "&view=3" +
									   "&view2=" + 
									   playerNumber +
									   "&action=View",
									   playerName,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				Routines.WriteHTMLLink(request,
									   response,
									   webPageOutput,
									   "Drafts",
									   "league=" +
									   league +
									   "&view=2" +
									   "&view2=" + 
									   positionNumber +
									   "&action=View",
									   positionName,
									   "bg2",
									   true);
				webPageOutput.println("    </NOBR></TD>");
				webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
				webPageOutput.println("</TR>");
				}
			  }
			else
			  {
			  for(int currentLoop=0;currentLoop<2;currentLoop++)
			   {
			   if(currentLoop==0)
			     {	
				 queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,PositionName,positions.PositionNumber,players.TeamNumber,DraftedRound,DraftedPick," +
				                                "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                                "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                                "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                                "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                                "Skill16,Skill17,Skill18,Skill19,Skill20 " +
												"FROM players,positions " +
												"WHERE players.CollegeNumber = " + college + " " +
												"AND players.Experience=0 " +
												"AND players.PositionNumber=positions.PositionNumber " +
												"AND players.WorldNumber=" + league + " " +
												"AND players.TeamNumber!=0 " +
					                            "AND Type!=3 " +
												"ORDER BY DraftedRound ASC, DraftedPick ASC");
				 }
			   else
				 {  	
				 queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,PositionName,positions.PositionNumber,players.TeamNumber,DraftedRound,DraftedPick," +
				                                "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
				                                "Skill1,Skill2,Skill3,Skill4,Skill5," +
				                                "Skill6,Skill7,Skill8,Skill9,Skill10," +
				                                "Skill11,Skill12,Skill13,Skill14,Skill15," +
				                                "Skill16,Skill17,Skill18,Skill19,Skill20 " +
												"FROM players,positions " +
												"WHERE players.CollegeNumber = " + college + " " +
												"AND players.Experience=0 " +
												"AND players.PositionNumber=positions.PositionNumber " +
												"AND players.WorldNumber=" + league + " " +
												"AND players.TeamNumber=0 " +	
					                            "AND Type!=3 " +											
												"ORDER BY Surname ASC, Forname ASC");			     										        
				 }
			   int playerNumber=0;
			   String playerName="";
			   positionName="";
			   int positionNumber=0;
			   int teamNumber=0;
			   teamName="";							     			  
			   int overallPick=0;
			   round=0;
			   int pick=0;	
			   while(queryResult1.next())
				 {
				 playerNumber=queryResult1.getInt(1);
				 playerName=queryResult1.getString(3) + "," + queryResult1.getString(2);
				 positionName=queryResult1.getString(4);
				 positionNumber=queryResult1.getInt(5);
				 teamNumber=queryResult1.getInt(6);	
				 round=queryResult1.getInt(7);
				 pick=queryResult1.getInt(8);
				 int skills[]=new int[25];
				 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					{
					skills[currentSkill]=queryResult1.getInt(9+currentSkill);
					}
				 int totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);	
				 teamName="";
				 overallPick=0;
				 if(teamNumber!=0)
				   {
				   overallPick=((round-1)*numOfTeams)+pick;
				   queryResult2=sql2.executeQuery("SELECT Name " +
												  "FROM teams " +
												  "WHERE TeamNumber="+teamNumber);
				   if(queryResult2.first())
					 {
					 teamName=queryResult2.getString(1);							     
					 }
				   }						     						 
				 webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				 if(teamNumber==0)
				   {
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\"></TD>");	
				   }
				 else
				   {
				   webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				   Routines.WriteHTMLLink(request,
										  response,
										  webPageOutput,
										  "Drafts",
										  "league=" +
										  league +
										  "&view=0" +
										  "&view2=" + 
										  round +
										  "&action=View",
										  String.valueOf(round),
										  "bg2",
										  true);
				   webPageOutput.println("    </NOBR></TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
				   webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");				  	
 			       webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				   Routines.WriteHTMLLink(request,
										  response,
										  webPageOutput,
										  "Drafts",
										  "league=" +
										  league +
										  "&view=1" +
										  "&view2=" + 
										  teamNumber +
					                      "&team=" +
										  teamNumber +
										  "&action=View",
										  teamName,
										  "bg2",
										  true);
				   webPageOutput.println("    </NOBR></TD>");
				   }
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Players",
										"league=" +
										league +
										"&view=3" +
										"&view2=" + 
										playerNumber +
										"&action=View",
										playerName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Drafts",
										"league=" +
										league +
										"&view=2" +
										"&view2=" + 
										positionNumber +
										"&action=View",
										positionName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
				 webPageOutput.println("</TR>");
				 }
			   }   
			}			
		  }
		  catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Error getting College order : " + error,false,context);
			  }
			}  		
		//By Alphabetical View
		if(view==4)
		  {
		  webPageOutput.println(Routines.spaceLines(2));
		  webPageOutput.println("<DIV CLASS=\"SLTables2\">");
		  webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
		  webPageOutput.println("<TBODY>");
		  webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
		  webPageOutput.println("<TD CLASS=\"bg0\" COLSPAN=\"8\"><FONT CLASS=\"bg0font\">"+letterChar+"</FONT></TD>");
		  webPageOutput.println("</TR>");
		  webPageOutput.println("<TR ALIGN=\"center\" class=\"bg4\">");
		  webPageOutput.println("   <TD ALIGN=\"left\">Round</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Pick</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Overall</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Team</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Player</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Position</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">College</TD>");
		  webPageOutput.println("   <TD ALIGN=\"left\">Rating</TD>");
		  webPageOutput.println("</TR>");
		  try
			{
			Statement sql1=database.createStatement();
			ResultSet queryResult1;
			Statement sql2=database.createStatement();
			ResultSet queryResult2;			
			int sequence=0;
		    queryResult1=sql1.executeQuery("SELECT players.PlayerNumber,Forname,Surname,PositionName,positions.PositionNumber,players.TeamNumber,DraftedRound,DraftedPick,colleges.CollegeNumber,CollegeName," +
			                               "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
			                               "Skill1,Skill2,Skill3,Skill4,Skill5," +
			                               "Skill6,Skill7,Skill8,Skill9,Skill10," +
			                               "Skill11,Skill12,Skill13,Skill14,Skill15," +
			                               "Skill16,Skill17,Skill18,Skill19,Skill20 " +
										   "FROM players,positions,colleges " +
										   "WHERE players.Surname LIKE '" + letterChar + "%' " +
										   "AND players.Experience=0 " +
										   "AND players.CollegeNumber=colleges.CollegeNumber " +
									       "AND players.PositionNumber=positions.PositionNumber " +
										   "AND players.WorldNumber=" + league + " " +
										   "AND Type!=3 " +
									       "ORDER BY SurName ASC,ForName ASC");
			int playerNumber=0;
			String playerName="";
			positionName="";
			int positionNumber=0;
			int teamNumber=0;
			teamName="";							     			  
			int overallPick=0;
			round=0;
			int pick=0;	
			int collegeNumber=0;
			collegeName="";
			while(queryResult1.next())
				 {
				 playerNumber=queryResult1.getInt(1);
				 playerName=queryResult1.getString(3) + "," + queryResult1.getString(2);
				 positionName=queryResult1.getString(4);
				 positionNumber=queryResult1.getInt(5);
				 teamNumber=queryResult1.getInt(6);	
				 round=queryResult1.getInt(7);
				 pick=queryResult1.getInt(8);
				 collegeNumber=queryResult1.getInt(9);
				 collegeName=queryResult1.getString(10);
				 int skills[]=new int[25];
				 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
					{
					skills[currentSkill]=queryResult1.getInt(11+currentSkill);
					}
				 int totalSkills = Routines.getSkillRating((positionSkills[positionNumber]),skills);	
				 teamName="";
				 overallPick=0;
				 if(teamNumber!=0)
				   {
				   overallPick=((round-1)*numOfTeams)+pick;
				   queryResult2=sql2.executeQuery("SELECT Name " +
												  "FROM teams " +
												  "WHERE TeamNumber="+teamNumber);
				   if(queryResult2.first())
					 {
					 teamName=queryResult2.getString(1);							     
					 }
				   }						     						 
				   webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\" VALIGN=\"middle\">");
				   if(teamNumber==0)
					 {
					 webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					 webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					 webPageOutput.println("   <TD ALIGN=\"left\"></TD>");
					 webPageOutput.println("   <TD ALIGN=\"left\"></TD>");	
					 }
				   else
					 {
					 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
					 Routines.WriteHTMLLink(request,
											response,
											webPageOutput,
											"Drafts",
											"league=" +
											league +
											"&view=0" +
											"&view2=" + 
											round +
											"&action=View",
											String.valueOf(round),
											"bg2",
											true);
					 webPageOutput.println("    </NOBR></TD>");
					 webPageOutput.println("   <TD ALIGN=\"left\">"+pick+"</TD>");
					 webPageOutput.println("   <TD ALIGN=\"left\">"+overallPick+"</TD>");				  	
					 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
					 Routines.WriteHTMLLink(request,
											response,
											webPageOutput,
											"Drafts",
											"league=" +
											league +
											"&view=1" +
											"&view2=" + 
											teamNumber +
						                    "&team=" +
											teamNumber +
											"&action=View",
											teamName,
											"bg2",
											true);
					 webPageOutput.println("    </NOBR></TD>");
					 }
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Players",
										"league=" +
										league +
										"&view=2" +
										"&view2=" + 
										playerNumber +
										"&action=View",
										playerName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Drafts",
										"league=" +
										league +
										"&view=2" +
										"&view2=" + 
										positionNumber +
										"&action=View",
										positionName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>");
				 Routines.WriteHTMLLink(request,
										response,
										webPageOutput,
										"Drafts",
										"league=" +
										league +
										"&view=3" +
										"&view2=" + 
										collegeNumber +
										"&action=View",
										collegeName,
										"bg2",
										true);
				 webPageOutput.println("    </NOBR></TD>");
				 webPageOutput.println("   <TD ALIGN=\"left\"><NOBR>"+Routines.skillsDescription((totalSkills)/10)+"</NOBR></TD>");
				 webPageOutput.println("</TR>");
				 }
		  }
		  catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Error getting Alphabetical order : " + error,false,context);
			  }
			}  													
	  webPageOutput.println("</TBODY>");
	  webPageOutput.println("</TABLE>");
	  webPageOutput.println("</DIV>");        
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + league + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"view\" VALUE=\"" + view + "\">");
      webPageOutput.println("</FORM>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }