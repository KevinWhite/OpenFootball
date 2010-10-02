import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Roster extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Roster";

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
      int leagueNumber=Routines.safeParseInt((String)session.getAttribute("league"));
	  int leagueType=Routines.safeParseInt((String)session.getAttribute("leagueType"));
	  int rosterMin=Routines.safeParseInt((String)session.getAttribute("rosterMin"));
	  int rosterMax=Routines.safeParseInt((String)session.getAttribute("rosterMax"));
	  int salaryCap=Routines.safeParseInt((String)session.getAttribute("salaryCap"));
	  int capHit=0;
	  int playerCount=0;
	  int salaryCount=0;
	  Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead("Roster",//title
                             true,//showMenu
                             6,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             true,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
	  int myTeam=Routines.doIHaveAVerifiedTeamInThisLeague(leagueNumber,database,session,request,context);
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      String action=request.getParameter("action");
      if(action==null)
        {
        action="";	
        }
      String sortText="";  
	  int[] positionSkills=Routines.getNumOfSkills(database,context);                       
      // Retrieve Player information from DB
      try
         {
         // Prepare screen
         if(leagueType!=2)
           {
           Routines.titleHeader("head2",true,webPageOutput);
           webPageOutput.println("Roster");
           Routines.titleTrailer(true,webPageOutput);
           }
         else
           {
		   webPageOutput.println("<BR>");	  
           }
         webPageOutput.println("<DIV CLASS=\"SLTables1\"><TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
         webPageOutput.println("<TBODY>");
         webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
         webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"10\">");
         webPageOutput.println("<FONT CLASS=\"home\">");
		 webPageOutput.println("<span style=\"font:3pt\">");
		 webPageOutput.println("<form>");
		 webPageOutput.println("<select name=\"pulldown\" onChange=\" window.location.href=this.options[this.selectedIndex].value\" >");
		 Statement sql = database.createStatement();
		 ResultSet rs = sql.executeQuery("SELECT Name,NickName,teams.TeamNumber,CapHit " +
										 "FROM conferences,divisions,leagueteams,teams " +
										 "WHERE LeagueNumber = " + leagueNumber + " " +
										 "AND conferences.ConferenceNumber = divisions.ConferenceNumber " +
										 "AND divisions.DivisionNumber = leagueteams.DivisionNumber " +
										 "AND leagueteams.TeamNumber = teams.TeamNumber " +
										 "ORDER BY Name,NickName ASC");
		 while(rs.next())
		    {	
		    String selected="";
		    if(rs.getInt(3)==teamNumber)
		      {
		      selected="selected ";	
		      capHit=rs.getInt(4);
		      }
			String strURL=Routines.encodeURL(request,response,"Roster","league="+leagueNumber+"&team="+rs.getInt(3));  
		    webPageOutput.println("<option "+selected+"value=\""+strURL+"\">"+rs.getString(1)+" "+rs.getString(2));								  
		    }
		 webPageOutput.println("</select>");
		 webPageOutput.println("</form>");
		 webPageOutput.println("</span>");
         webPageOutput.println("Players");
         webPageOutput.println("</FONT>");
         webPageOutput.println("</TD>");
         webPageOutput.println("</TR>");
         webPageOutput.println("<TR ALIGN=\"left|left|left|left|left|left|left|left|left\" CLASS=\"bg1\">");
         webPageOutput.println("<TD>");
         if(action.equals("sortNo"))
           {
		   webPageOutput.println("No");	
		   sortText=" ORDER BY Number ASC,Surname ASC,Forname ASC";
           }
         else
           {  
           Routines.WriteHTMLLink(request,
			 					  response,
								  webPageOutput,
								  "Roster",
								  "action=sortNo&league="+leagueNumber+"&team="+teamNumber,
								  "No",
								  null,
								  true);
           }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortName")||action.equals(""))
		   {
		   webPageOutput.println("Name");
		   sortText=" ORDER BY Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortName&league="+leagueNumber+"&team="+teamNumber,
								  "Name",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
         if(action.equals("sortPos"))
		   {
		   webPageOutput.println("Pos");
		   sortText=" ORDER BY positions.Type ASC,positions.Sequence ASC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortPos&league="+leagueNumber+"&team="+teamNumber,
								  "Pos",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortValue"))
		   {
		   webPageOutput.println("Contract");
		   sortText=" ORDER BY ContractValue DESC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortValue&league="+leagueNumber+"&team="+teamNumber,
								  "Contract",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortLength"))
		   {
		   webPageOutput.println("SignedFor");
		   sortText=" ORDER BY ContractLength DESC,ContractValue DESC, Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortLength&league="+leagueNumber+"&team="+teamNumber,
								  "SignedFor",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortHeight"))
		   {
		   webPageOutput.println("Height");
		   sortText=" ORDER BY Height DESC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortHeight&league="+leagueNumber+"&team="+teamNumber,
								  "Height",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortWeight"))
		   {
		   webPageOutput.println("Weight");
		   sortText=" ORDER BY Weight DESC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortWeight&league="+leagueNumber+"&team="+teamNumber,
								  "Weight",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortRating"))
		   {
		   webPageOutput.println("Rating");
		   sortText=" ORDER BY 4 ASC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortRating&league="+leagueNumber+"&team="+teamNumber,
								  "Rating",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortExp"))
		   {
		   webPageOutput.println("Exp");
		   sortText=" ORDER BY Experience DESC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortExp&&league="+leagueNumber+"&team="+teamNumber,
								  "Exp",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
		 webPageOutput.println("<TD>");
		 if(action.equals("sortCollege"))
		   {
		   webPageOutput.println("College");
		   sortText=" ORDER BY CollegeName ASC,Surname ASC,Forname ASC";	
		   }
		 else
		   {  
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "Roster",
								  "action=sortCollege&league="+leagueNumber+"&team="+teamNumber,
								  "College",
								  null,
								  true);
		   }						  
		 webPageOutput.println("</TD>");
         webPageOutput.println("</TR>");
		 sql = database.createStatement();
		 rs  = sql.executeQuery("SELECT Surname,Forname,Experience,Number,Height,Weight,CollegeName," +
								"Skill1,Skill2,Skill3,Skill4,Skill5," +
								"Skill6,Skill7,Skill8,Skill9,Skill10," +
								"Skill11,Skill12,Skill13,Skill14,Skill15," +
								"Skill16,Skill17,Skill18,Skill19,Skill20," +
								"players.PlayerNumber,PositionCode,masterplayers.PositionNumber,RealNumber,ContractValue,ContractLength " +
								"FROM leagues,masterplayers,players,colleges,positions,positionskills " +
								"WHERE TeamNumber = " + teamNumber + " " +
								"AND players.WorldNumber = leagues.LeagueNumber " +
								"AND players.MasterPlayerNumber = masterplayers.MasterPlayerNumber " +
								"AND masterplayers.Season = leagues.PlayerSeason " +
								"AND masterplayers.CollegeNumber = colleges.CollegeNumber " +
								"AND masterplayers.PositionNumber = positions.PositionNumber " +
								"AND Type < 3 " +
								"AND masterplayers.PositionNumber = positionskills.PositionNumber " +
								"GROUP BY players.PlayerNumber " +
								sortText);
         // Print details of each player.
         if(rs.first())
           {
           rs.beforeFirst();
           }
         else
           {
           webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
           webPageOutput.println("<TD ALIGN=\"left\" COLSPAN=\"10\">None.</TD>");
           webPageOutput.println("</TR>");
           }
         while(rs.next())
            {
            String surName      = rs.getString(1);
            String forName      = rs.getString(2);
            int    experience   = rs.getInt(3);
            int    number       = rs.getInt(4);
            int    height       = rs.getInt(5);
            int    heightFeet = height/12;
            int    heightInches = height-(heightFeet*12);
            int    weight       = rs.getInt(6);
            String collegeName  = rs.getString(7);
			int skills[]=new int[20];
			for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
			   {
			   skills[currentSkill]=rs.getInt(8+currentSkill);
			   }
			int playerNumber     = rs.getInt(28);
			String positionCode  = rs.getString(29);
			int positionNumber   = rs.getInt(30);
			int realNumber       = rs.getInt(31);
			int contractValue    = rs.getInt(32);
			int contractLength   = rs.getInt(33);
			int totalSkills      = Routines.getSkillRating((positionSkills[positionNumber]),skills);	
			playerCount++;
			salaryCount+=contractValue;
            webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  number +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">");
            Routines.writeExternalHTMLLink(webPageOutput,
                                           "http://www.nfl.com/players/playerpage/"+realNumber,
                                           (surName + ", " + forName),
                                           null,
                                           true);
            webPageOutput.println("</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  positionCode +
                                  "</TD>");
 			webPageOutput.println("    <TD ALIGN=\"left\">" +
								  "$" + ((double)(contractValue)/1000) + "m" +
								  "</TD>");
			webPageOutput.println("    <TD ALIGN=\"left\">" +
								  contractLength + "yrs" +
								  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  heightFeet +
                                  "'" +
                                  heightInches +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  weight +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
			                      Routines.skillsDescription((totalSkills)/10) +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  experience +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  collegeName +
                                  "</TD>");
            webPageOutput.println("</TR>");

            }
		 webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg1\">");
		 webPageOutput.println("<TD COLSPAN=\"10\"><B>");
		 String playerText="";
		 String capText="";
		 if(playerCount<rosterMin)
		   {
		   playerText="<FONT COLOR=\"red\"> (" + (rosterMin-playerCount) + " under limit)</FONT>";	
		   }
		 if(playerCount>rosterMax)
		   {
		   playerText="<FONT COLOR=\"red\"> (" + (playerCount-rosterMax) + " over limit)</FONT>";	
		   }
		 if((salaryCount*1000)+capHit<salaryCap)
		   {
		  capText=" (";	
		   if(capHit!=0)
		     {	
		     capText+="with $" + (double)capHit/1000000+"m cap hit, makes a total of ";	
		     }
		   capText+="$" + ((double)(((salaryCap-capHit)/1000)-salaryCount))/1000+"m under cap)";  
		   }  
		 if((salaryCount*1000)+capHit>salaryCap)
		   {
		   capText="<FONT COLOR=\"red\"> (";	
		   if(capHit!=0)
			 {	
			 capText+="with $" + (double)capHit/1000000+"m cap hit, makes a total of ";	
			 }
		   capText+="$" + ((double)(salaryCount+(capHit/1000)-(salaryCap/1000)))/1000+"m over cap)</FONT>";	
		   }		   		   
		 webPageOutput.println("Total of " + playerCount + " players" + playerText + " for $" +((double)(salaryCount)/1000)+"m"+capText);
		 webPageOutput.println("</B></TD>");
		 webPageOutput.println("</TR>");
         webPageOutput.println("</TBODY></TABLE></DIV>");
         webPageOutput.println(Routines.spaceLines(2));
         rs  = sql.executeQuery("SELECT Surname,Forname,Experience,Skill1,players.PlayerNumber,PositionName " +
                                "FROM masterplayers,players,positions " +
                                "WHERE TeamNumber = " +
                                Routines.safeParseInt((String)session.getAttribute("team")) + " " +
                                "AND players.MasterPlayerNumber=masterplayers.MasterPlayerNumber " +
                                "AND masterplayers.PositionNumber=positions.PositionNumber " +
                                "AND positions.Type=3 " +
                                "ORDER BY Surname ASC,Forname ASC");
         webPageOutput.println("<DIV CLASS=\"SLTables1\"><TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
         webPageOutput.println("<TBODY>");
         webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
         webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"4\">");
         webPageOutput.println("<FONT CLASS=\"home\">");
         webPageOutput.println("Staff");
         webPageOutput.println("</FONT>");
         webPageOutput.println("</TD>");
         webPageOutput.println("</TR>");
         webPageOutput.println("<TR ALIGN=\"left|left|left|left\" CLASS=\"bg1\">");
         webPageOutput.println("<TD>Position</TD>");
         webPageOutput.println("<TD>Name</TD>");
         webPageOutput.println("<TD>Experience</TD>");
         webPageOutput.println("<TD>Rating</TD>");
         webPageOutput.println("</TD>");
         webPageOutput.println("</TR>");

         // Print details of each player.
         String positionText="";
         if(rs.first())
           {
           rs.beforeFirst();
           }
         else
           {
           webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
           webPageOutput.println("<TD ALIGN=\"left\" COLSPAN=\"4\">None.</TD>");
           webPageOutput.println("</TR>");
           }
         while(rs.next())
            {
            String surName      = rs.getString(1);
            String forName      = rs.getString(2);
            int    experience   = rs.getInt(3);
            int    skills       = (rs.getInt(4)+5)/10;
            int    playerNumber = rs.getInt(5);
            positionText = rs.getString(6);
            webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  positionText +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">");
            Routines.WriteHTMLLink(request,
                                   response,
                                   webPageOutput,
                                   "wfafl",
                                   "action=viewPlayer" +
                                   "&value=" +
                                   playerNumber,
                                   (surName + "," + forName),
                                   null,
                                   true);
            webPageOutput.println("</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  experience +
                                  "</TD>");
            webPageOutput.println("    <TD ALIGN=\"left\">" +
                                  Routines.skillsDescription(skills) +
                                  "</TD>");
            webPageOutput.println("</TR>");
            }
         webPageOutput.println("</TBODY></TABLE></DIV>");
         webPageOutput.println(Routines.spaceLines(1));
         Routines.WriteHTMLTail(request,response,webPageOutput);
         }
      catch(SQLException error)
         {
         Routines.writeToLog(servletName,"Database error retrieving rosters : " + error,false,context);
         }
      finally
         {
         pool.returnConnection(database);
         }
      }
   }