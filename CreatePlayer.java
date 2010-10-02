import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.*;

public class CreatePlayer extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="CreatePlayer";

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
      int playerNumber=Routines.safeParseInt(request.getParameter("playerNumber"));
	  int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      try
        {
        webPageOutput=response.getWriter();
        }
      catch(IOException error)
        {
        Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
        }
      HttpSession session=request.getSession();
      String action=request.getParameter("action");
      String disabledText="";
      if("New Player".equals(action)||
         "Change Player".equals(action))
         {
         disabledText=" DISABLED";
         }
      else
        {
        session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
        }
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("Create Player",//title
                             false,//showMenu
                             11,//menuHighLight
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
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
	  int numOfPositions=0;
	  String positionNames[]=null;
	  int positionNumbers[]=null; 	 
      //Load Positions
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionName,PositionNumber " +
                                     "FROM positions " +
                                     "WHERE RealPosition=1 " +
                                     "ORDER BY Type,Sequence ASC");
		while(queryResult.next())
			 {
			 numOfPositions++;
			 }
		positionNumbers=new int[numOfPositions];
		positionNames=new String[numOfPositions];	 
  	    queryResult.beforeFirst();
		int currentPosition=0;		 
        while(queryResult.next())
             {
             positionNames[currentPosition]=queryResult.getString(1);
             positionNumbers[currentPosition]=queryResult.getInt(2);
			 currentPosition++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);
        }
      if("Change Player".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayerNumber " +
                                       "FROM masterplayers ");
          while(queryResult.next())
               {
               int tempPlayerNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(tempPlayerNumber))))
                 {
                 changeCount++;
				 playerNumber=queryResult.getInt(1);
                 if(!changeRequested)
                   {
                   changeRequested=true;
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to find masterplayer entries : " + error,false,context);
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No player selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one player to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store Player".equals(action)||
          "Update Player".equals(action)||
          "Delete Player".equals(action)||
		  "Clone Player".equals(action))
          {
          updated=updateEntry(positionNumber,
                              action,
                              session,
                              request,
                              database);
          }
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/CreatePlayer\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Position",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"positionNumber\">");
      int selectedPositionNumber=0;
      for(int currentPosition=0;currentPosition<numOfPositions;currentPosition++)
         {
         String selectedPosition="";	
         if(positionNumber==0)
           {
           positionNumber=positionNumbers[currentPosition];	
           }
		 int tempPositionNumber=positionNumbers[currentPosition];
		 if(positionNumber==tempPositionNumber)
			  {
			  selectedPosition=" SELECTED";
			  selectedPositionNumber=currentPosition;
			  }
			else
			  {
			  selectedPosition="";
			  }
			webPageOutput.println(" <OPTION" + disabledText + selectedPosition + " VALUE=\"" + positionNumbers[currentPosition] + "\">" + positionNames[currentPosition]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT" + disabledText + " TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      if((String)session.getAttribute("message")!=null)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Messages",0,webPageOutput);
        Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
        Routines.messageCheck(false,request,webPageOutput);
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        }
      Routines.tableStart(false,webPageOutput);
      String titleText="";
	  if("New Player".equals(action))
		{
		titleText="Enter Player Details";	
		}
	  if("Change Player".equals(action))
		{
		titleText="Change Player Details";	
		}
	  if(!"New Player".equals(action)&&
		 !"Change Player".equals(action))
		{
		titleText=positionNames[selectedPositionNumber] + "s found on system";	
		}
	  Routines.tableHeader(titleText,19,webPageOutput);
	  if(!"New Player".equals(action)&&
	     !"Change Player".equals(action))
		{
        boolean playerFound=false;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayerNumber," +
                                       "Surname,Forname,Season " +
                                       "FROM masterplayers " +
                                       "WHERE PositionNumber=" + positionNumber  + " " +
                                       "ORDER BY Surname,Forname,Season ASC");
          int currentPlayerNumber=0;
          String currentSurname="";
          String currentForname="";
          int currentSeason=0;
          while(queryResult.next())
               {
               if(!playerFound)
                 {
                 playerFound=true;
                 }
               currentPlayerNumber=queryResult.getInt(1);
               currentSurname=queryResult.getString(2);
			   currentForname=queryResult.getString(3);
               currentSeason=queryResult.getInt(4);
               Routines.tableDataStart(true,false,false,true,false,3,0,"scoresrow",webPageOutput);
               webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + currentPlayerNumber  + "\" VALUE=\"true\"");
               webPageOutput.println(">");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
               webPageOutput.println(currentForname+" "+currentSurname+"("+currentSeason+")");
               Routines.tableDataEnd(false,false,true,webPageOutput);
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to retrieve masterplayer entries : " + error,false,context);
          }
        if(!playerFound&&!"New Player".equals(action)&&!"Change Player".equals(action))
          {
          Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
          webPageOutput.println("No players found.");
          Routines.tableDataEnd(false,false,true,webPageOutput);
          }
		}
      Routines.tableEnd(webPageOutput);
      if("New Player".equals(action)||
         "Change Player".equals(action))
        {
        formLine(action,
                 positionNumber,
                 request,
                 webPageOutput,
                 database);
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if("New Player".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Player\" NAME=\"action\">");
        }
      if("Change Player".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Update Player\" NAME=\"action\">");
        }
      if("New Player".equals(action)||
         "Change Player".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
		webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"positionNumber\" VALUE=\"" + positionNumber + "\">");
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playerNumber\" VALUE=\"" + playerNumber + "\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Player\" NAME=\"action\">");
        if(!"New Player".equals(action)&&!"Change Player".equals(action))
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Player\" NAME=\"action\">");
		  webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Clone Player\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Player\" NAME=\"action\">");
          }
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void formLine(String action,
                         int positionNumber,
                         HttpServletRequest request,
                         PrintWriter webPageOutput,
                         Connection  database)
      {
	  Calendar dobCal=Calendar.getInstance();
	  int playerNumber=0;
	  int[] collegeNumbers=null;
      String[] collegeNames=null;	
      int currentCollege=0;
	  int college=0;
	  String surName="";
	  String forName="";
	  int experience=0;
	  int preferredNumber=0;
	  int height=0;
	  int weight=0;
	  int[] skills=new int[20];
	  int realNumber=0;
	  int season=0;
	  int day=0;
	  int month=0;
	  int year=0;
	  if("Change Player".equals(action))
        {
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT PlayerNumber " +
									   "FROM masterplayers " +
									   "WHERE PositionNumber="+positionNumber);
		  while(queryResult.next())
			   {
			   int tempPlayerNumber=queryResult.getInt(1);
			   if("true".equals(request.getParameter(String.valueOf(tempPlayerNumber))))
				 {
				 playerNumber=tempPlayerNumber;	
				 }
			   }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to find player : " + error,false,context);
		  }
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT " +
									   "CollegeNumber,PositionNumber,Surname,Forname,Experience," +
									   "PreferredNumber,Height,Weight," +
									   "Skill1,Skill2,Skill3,Skill4,Skill5," +
									   "Skill6,Skill7,Skill8,Skill9,Skill10," +
									   "Skill11,Skill12,Skill13,Skill14,Skill15," +
									   "Skill16,Skill17,Skill18,Skill19,Skill20," +
									   "RealNumber,Season,DOB " +
									   "FROM masterplayers " +
									   "WHERE PlayerNumber=" + playerNumber);
			  while(queryResult.next())
				   {
				   college=queryResult.getInt(1);
				   positionNumber=queryResult.getInt(2);
				   surName=queryResult.getString(3);
				   forName=queryResult.getString(4);
				   experience=queryResult.getInt(5);
				   preferredNumber=queryResult.getInt(6);
				   height=queryResult.getInt(7);
				   weight=queryResult.getInt(8);
				   for(int currentSkill=0;currentSkill<20;currentSkill++)
					  {
					  skills[currentSkill]=queryResult.getInt(9+currentSkill);
					  }
				   realNumber=queryResult.getInt(29);
				   season=queryResult.getInt(30);
				   java.util.Date calDate=queryResult.getDate(31);
				   dobCal.setTime(calDate);
				   year=dobCal.get(Calendar.YEAR);
				   month=dobCal.get(Calendar.MONTH);
				   day=dobCal.get(Calendar.DAY_OF_MONTH);
				   }
			  }
			catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Unable to retrieve masterplayer : " + error,false,context);
			  }        	
        }
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT CollegeNumber,CollegeName " +
								     "FROM colleges " +
								     "ORDER BY CollegeName ASC");
		while(queryResult.next())
			 {
			 currentCollege++;
			 }
		collegeNumbers=new int[currentCollege];
		collegeNames=new String[currentCollege];	 
		queryResult.beforeFirst();
		currentCollege=0;		 
		while(queryResult.next())
			 {
			 collegeNumbers[currentCollege]=queryResult.getInt(1);
			 collegeNames[currentCollege]=queryResult.getString(2);	
			 currentCollege++;
			 }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to find colleges : " + error,false,context);
		}
	  int[] skillNumbers=null;
	  String[] skillNames=null;	
	  int[] entries=null;
	  int currentSkill=0;
	  try
	    {
	    Statement sql=database.createStatement();
	    ResultSet queryResult;
	    queryResult=sql.executeQuery("SELECT skills.SkillNumber,skillName,Entries " +
									 "FROM positionskills,skills " +
									 "WHERE positionskills.PositionNumber=" + positionNumber + " " +
									 "AND positionskills.SkillNumber=skills.SkillNumber " +
									 "ORDER BY positionskills.Sequence ASC");
		while(queryResult.next())
			 {
			 currentSkill++;
			 }
		skillNumbers=new int[currentSkill];
		skillNames=new String[currentSkill];	
		entries=new int[currentSkill]; 
		queryResult.beforeFirst();
		currentSkill=0;		 
		while(queryResult.next())
			 {
			 skillNumbers[currentSkill]=queryResult.getInt(1);
			 skillNames[currentSkill]=queryResult.getString(2);	
			 entries[currentSkill]=queryResult.getInt(3);
			 currentSkill++;
			 }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to find skills : " + error,false,context);
		}		
      String months[]={"January","February","March","April","May","June",
      	               "July","August","September","October","November","December"};	
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
      webPageOutput.println("Forname");
      Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"forName\" SIZE=\"20\" MAXLENGTH=\"50\" VALUE=\"" + forName + "\">");
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
      webPageOutput.println("Surname");
      Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"surName\" SIZE=\"20\" MAXLENGTH=\"50\" VALUE=\"" + surName + "\">");
	  Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
      webPageOutput.println("Season");
      Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"season\">");
	  for(int currentYear=2005;currentYear<2010;currentYear++)
	     {
	     String selected="";	
		 if(currentYear==season)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentYear+"\">"+currentYear);	
	     }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
      webPageOutput.println("Date Of Birth");
      Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,15,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"day\">");
	  for(int currentDay=1;currentDay<32;currentDay++)
	     {
		 String selected="";	
		 if(currentDay==day)
 		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
	     webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentDay+"\">"+currentDay);	
	     }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<SELECT NAME=\"month\">");
	  for(int currentMonth=0;currentMonth<12;currentMonth++)
		 {
		 String selected="";	
		 if(currentMonth==month)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentMonth+"\">"+months[currentMonth]);	
		 }
	  webPageOutput.println("</SELECT>");
	  webPageOutput.println("<SELECT NAME=\"year\">");
	  for(int currentYear=1960;currentYear<2010;currentYear++)
		 {
		 String selected="";	
		 if(currentYear==year)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentYear+"\">"+currentYear);	
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
      webPageOutput.println("College");
      Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"college\">");
	  for(currentCollege=0;currentCollege<collegeNumbers.length;currentCollege++)
		 {
		 String selected="";	
		 if(collegeNumbers[currentCollege]==college)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+collegeNumbers[currentCollege]+"\">"+collegeNames[currentCollege]);	
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("Experience");
	  Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"experience\">");
	  for(int currentYear=0;currentYear<30;currentYear++)
		 {
		 String selected="";	
		 if(currentYear==experience)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentYear+"\">"+currentYear);	
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("Preferred Number");
	  Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"preferredNumber\">");
	  for(int currentNumber=0;currentNumber<100;currentNumber++)
		 {
		 String selected="";	
		 if(currentNumber==preferredNumber)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentNumber+"\">"+currentNumber);	
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("Height");
	  Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"height\">");
	  int currentHeight=60;
	  for(int currentFeet=5;currentFeet<8;currentFeet++)
		 {
		 for(int currentInches=0;currentInches<12;currentInches++)
		    {	
			String selected="";	
			if(currentHeight==height)
			  {
			  selected="SELECTED";
			  }
			else
			  {
			  selected="";
			  }
			webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentHeight+"\">"+currentFeet+"ft "+currentInches+"\"");
			currentHeight++;
		    }
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("Weight");
	  Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"weight\">");
	  for(int currentWeight=150;currentWeight<500;currentWeight++)
		 {
		 String selected="";	
		 if(currentWeight==weight)
		   {
		   selected="SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println("<OPTION "+selected+" VALUE=\""+currentWeight+"\">"+currentWeight+"lbs");
		 }
	  webPageOutput.println("</SELECT>");	  
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("Real Number");
	  Routines.tableDataEnd(true,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"realNumber\" SIZE=\"10\" MAXLENGTH=\"10\" VALUE=\"" + realNumber + "\">");
	  Routines.tableDataEnd(true,true,true,webPageOutput);
	  for(currentSkill=0;currentSkill<skillNumbers.length;currentSkill++)
	      {
		  Routines.tableDataStart(true,false,true,true,true,50,0,"scoresrow",webPageOutput);
		  webPageOutput.println(skillNames[currentSkill]);
		  Routines.tableDataEnd(true,false,false,webPageOutput);
		  Routines.tableDataStart(true,false,true,false,false,50,0,"scoresrow",webPageOutput);
		  webPageOutput.println("<SELECT NAME=\""+skillNumbers[currentSkill]+"\">");
		  try
			{
			Statement sql=database.createStatement();
			ResultSet queryResult;
			queryResult=sql.executeQuery("SELECT Cost,Value1,Value2,Value3,Value4,Value5,Value6," +
			                             "Value7,Value8,Value9,Value10,Value11,Value12 " +
										 "FROM skilltables " +
										 "WHERE SkillNumber=" + skillNumbers[currentSkill] + " " +
										 "ORDER BY Cost DESC");
			int cost=0;
			while(queryResult.next())
			     {
				 String values="";
		  	     cost=queryResult.getInt(1);	
				 for(int currentItem=1;currentItem<entries[currentSkill]+1;currentItem++)
				     {	
				     if(currentItem>1)
				       {
				       values+=" ";		
				       }
					 values+=queryResult.getInt(currentItem+1);
				     }
				 String selected="";	
				 if(skills[currentSkill]==cost)
				   {
				   selected="SELECTED";
				   }
				 else
				   {
				   selected="";
				   }
				 webPageOutput.println("<OPTION "+selected+" VALUE=\""+cost+"\">"+values);
                 }
			}
		  catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Unable to find skilltables : " + error,false,context);
			}	
		  webPageOutput.println("</SELECT>");	  
		  Routines.tableDataEnd(true,true,true,webPageOutput);
		  }
          Routines.tableDataEnd(false,false,true,webPageOutput);
          Routines.tableEnd(webPageOutput);
      }

   private synchronized boolean updateEntry(int positionNumber,
                                            String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int playerNumber=Routines.safeParseInt(request.getParameter("playerNumber"));
	  String forName=request.getParameter("forName");
	  String surName=request.getParameter("surName");
	  int season=Routines.safeParseInt(request.getParameter("season"));
	  int day=Routines.safeParseInt(request.getParameter("day"));
	  int month=Routines.safeParseInt(request.getParameter("month"));
	  int year=Routines.safeParseInt(request.getParameter("year"));
	  int college=Routines.safeParseInt(request.getParameter("college"));
	  int experience=Routines.safeParseInt(request.getParameter("experience"));
	  int preferredNumber=Routines.safeParseInt(request.getParameter("preferredNumber"));
	  int height=Routines.safeParseInt(request.getParameter("height"));
	  int weight=Routines.safeParseInt(request.getParameter("weight"));
	  int realNumber=Routines.safeParseInt(request.getParameter("realNumber"));
	  int skills[]=new int[20];
	  int masterPlayerNumber=0;
	  Calendar dobCal=Calendar.getInstance();
	  dobCal.set(Calendar.YEAR,year);
	  dobCal.set(Calendar.MONTH,month);
	  dobCal.set(Calendar.DAY_OF_MONTH,day);
	  DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd");
	  String dobText=dbFormat.format(dobCal.getTime());
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT PlayerNumber " +
									 "FROM masterplayers " +
									 "ORDER BY PlayerNumber DESC");
		if(queryResult.first())
		  {
		  masterPlayerNumber=queryResult.getInt(1);
		  masterPlayerNumber++;
		  }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to find masterplayers : " + error,false,context);
		}
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT SkillNumber " +
									 "FROM positionskills " +
									 "WHERE positionNumber="+positionNumber + " " +
									 "ORDER BY Sequence ASC");
		int currentSkill=0;
		while(queryResult.next())
			 {
			 int skillNumber=queryResult.getInt(1);
		     skills[currentSkill]=Routines.safeParseInt(request.getParameter(String.valueOf(skillNumber)));
			 currentSkill++; 
			 }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to find positionskills : " + error,false,context);
		}
      if("Store Player".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO masterplayers (" +
                                    "CollegeNumber,PositionNumber," +
                                    "Surname,Forname,Experience,PreferredNumber," +
                                    "Height,Weight," +
                                    "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                    "Skill6,Skill7,Skill8,Skill9,Skill10," +
                                    "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                    "Skill16,Skill17,Skill18,Skill19,Skill20," +
                                    "RealNumber,MasterPlayerNumber,Season,DOB,DateTimeStamp) " +
                                    "VALUES (" +
                                    college + "," +
                                    positionNumber + ",'" +
                                    surName + "','" +
                                    forName + "'," +
                                    experience + "," +
                                    preferredNumber + "," +
                                    height + "," +
                                    weight + "," +
                                    skills[0] + "," +
									skills[1] + "," +
									skills[2] + "," +
									skills[3] + "," +
									skills[4] + "," +
									skills[5] + "," +
									skills[6] + "," +
									skills[7] + "," +
									skills[8] + "," +
									skills[9] + "," +
									skills[10] + "," +
									skills[11] + "," +
									skills[12] + "," +
									skills[13] + "," +
									skills[14] + "," +
									skills[15] + "," +
									skills[16] + "," +
									skills[17] + "," +
									skills[18] + "," +
			                        skills[19] + "," +
									realNumber + "," +
									masterPlayerNumber + "," +
									season + ",'" +
									dobText + "','" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"masterplayer not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create masterplayer : " + error,false,context);
          }
		try
		  {
		  Statement sql1=database.createStatement();
		  Statement sql2=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql1.executeQuery("SELECT WorldNumber "+
										"FROM leagues " +
										"WHERE leagues.PlayerSeason="+season);
		  while(queryResult.next())
			   {
			   int worldNumber=queryResult.getInt(1);
			   int updates=0;	
			   updates=sql2.executeUpdate("INSERT INTO players (" +
                                          "WorldNumber,MasterPlayerNumber,DateTimeStamp) " +
                                          "VALUES (" +
                                          worldNumber + "," +
                                          masterPlayerNumber + ",'" +
								          Routines.getDateTime(false) + "')");
				if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"players not created (" + masterPlayerNumber + ")",false,context);
				   }
			   }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to update players : " + error,false,context);
		  }          
        session.setAttribute("message",forName+" "+surName+" stored successfully");
        success=true;
        }
      if("Update Player".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE masterplayers " +
                                    "SET CollegeNumber=" + college + "," +
                                    "PositionNumber=" + positionNumber + "," +
                                    "Surname='" + surName + "'," +
                                    "Forname='" + forName + "'," +
									"Experience=" + experience + "," +
									"PreferredNumber=" + preferredNumber + "," +
									"Height=" + height + "," +
									"Weight=" + weight + "," +
									"Skill1=" + skills[0] + "," +
									"Skill2=" + skills[1] + "," +
									"Skill3=" + skills[2] + "," +
									"Skill4=" + skills[3] + "," +
									"Skill5=" + skills[4] + "," +
									"Skill6=" + skills[5] + "," +
									"Skill7=" + skills[6] + "," +
									"Skill8=" + skills[7] + "," +
									"Skill9=" + skills[8] + "," +
									"Skill10=" + skills[9] + "," +
									"Skill11=" + skills[10] + "," +
									"Skill12=" + skills[11] + "," +
									"Skill13=" + skills[12] + "," +
									"Skill14=" + skills[13] + "," +
									"Skill15=" + skills[14] + "," +
									"Skill16=" + skills[15] + "," +
									"Skill17=" + skills[16] + "," +
									"Skill18=" + skills[17] + "," +
									"Skill19=" + skills[18] + "," +
									"Skill20=" + skills[19] + "," +
									"RealNumber=" + realNumber + "," +
									"Season=" + season + "," +
									"DOB='" + dobText + "'," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE PlayerNumber=" + playerNumber);
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"masterplayer not updated, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update masterplayers : " + error,false,context);
          }  
        session.setAttribute("message",forName+" "+surName+" changed successfully");
        success=true;
        }
      if("Delete Player".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayerNumber " +
                                         "FROM masterplayers");
          while(queryResult1.next())
               {
               playerNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playerNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM masterplayers " +
                                            "WHERE PlayerNumber=" + playerNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,forName+" "+surName+" not deleted (" + playerNumber + ")",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete "+forName+" "+surName+" : " + error,false,context);
          }
        if(deleteRequested)
          {
          session.setAttribute("message",forName+" "+surName+" deleted successfully");
          }
        else
          {
          session.setAttribute("message","No player selected");
          }
        success=true;
        }
	  if("Clone Player".equals(action))
		{
		boolean cloneRequested=false;
		try
		  {
		  int updates=0;
		  Statement sql1=database.createStatement();
		  Statement sql2=database.createStatement();
		  ResultSet queryResult1;
		  ResultSet queryResult2;
		  queryResult1=sql1.executeQuery("SELECT PlayerNumber " +
										 "FROM masterplayers");
		  while(queryResult1.next())
			   {
			   playerNumber=queryResult1.getInt(1);
			   if("true".equals(request.getParameter(String.valueOf(playerNumber))))
				 {
				 if(!cloneRequested)
				   {
				   cloneRequested=true;
				   }
				 updates=sql2.executeUpdate("INSERT INTO masterplayers (" +
				                            "CollegeNumber,PositionNumber,Surname,Forname," +
				                            "Experience,PreferredNumber,Height,Weight," +
				                            "Skill1,Skill2,Skill3,Skill4,Skill5,Skill6,Skill7,Skill8,Skill9,Skill10," +
				                            "Skill11,Skill12,Skill13,Skill14,Skill15,Skill16,Skill17,Skill18,Skill19,Skill20," +
				                            "RealNumber,MasterPlayerNumber,Season,DOB) " +
				                            "SELECT CollegeNumber,PositionNumber,Surname,Forname," +
				                            "Experience,PreferredNumber,Height,Weight," +
				                            "Skill1,Skill2,Skill3,Skill4,Skill5,Skill6,Skill7,Skill8,Skill9,Skill10," +
				                            "Skill11,Skill12,Skill13,Skill14,Skill15,Skill16,Skill17,Skill18,Skill19,Skill20," +
				                            "RealNumber,MasterPlayerNumber,(Season+1),DOB FROM masterplayers " +
										    "WHERE PlayerNumber=" + playerNumber);
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,forName+" "+surName+" not cloned (" + playerNumber + ")",false,context);
				   }
				 }
			   }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to clone "+forName+" "+surName+" : " + error,false,context);
		  }
		if(cloneRequested)
		  {
		  session.setAttribute("message","Clone successfull");
		  }
		else
		  {
		  session.setAttribute("message","No player selected");
		  }
		success=true;
		}        
      return success;
      }
}