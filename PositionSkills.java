import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class PositionSkills extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="PositionSkills";

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
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int leagueNumber=Routines.safeParseInt(request.getParameter("leagueNumber"));
      int teamNumber=Routines.safeParseInt(request.getParameter("teamNumber"));
      String action=request.getParameter("action");
      String[] positions=null;
      int[] positionNumbers=null;
      String disabledText="";
      String positionName="";
      if("New Skill".equals(action)||
         "Change Skill".equals(action))
         {
         disabledText=" DISABLED";
         if(session.isNew())
           {
           session.setAttribute("redirect",
                                "http://" +
                                request.getServerName() +
                                ":" +
                                request.getServerPort() +
                                request.getContextPath() +
                                "/servlet/PositionSkills?jsessionid=" + session.getId() + "&league=" + leagueNumber + "&team=" + teamNumber);
           }
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
      Routines.WriteHTMLHead("Position Skills",//title
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
      int positionSkillsNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfPositions=0;
        queryResult=sql.executeQuery("SELECT PositionNumber,PositionName " +
                                     "FROM positions " +
                                     "WHERE RealPosition=1 " +
                                     "AND Type!=3 " +
                                     "ORDER BY PositionNumber ASC");
        while(queryResult.next())
             {
             if(positionNumber==0)
               {
               positionNumber=queryResult.getInt(1);
               }
             numOfPositions++;
             }
        positions=new String[numOfPositions];
        positionNumbers=new int[numOfPositions];
        if(numOfPositions>0)
          {
          numOfPositions=0;
          queryResult.beforeFirst();
          while(queryResult.next())
               {
               positionNumbers[numOfPositions]=queryResult.getInt(1);
               positions[numOfPositions]=queryResult.getString(2);
               numOfPositions++;
               }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);	
        }
      if("Change Skill".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionSkillNumber " +
                                       "FROM positionskills ");
          while(queryResult.next())
               {
               positionSkillsNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(positionSkillsNumber))))
                 {
                 changeCount++;
                 if(!changeRequested)
                   {
                   changeRequested=true;
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to find positionskills entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No skill selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one skill to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Skill".equals(action)||
          "Store Changed Skill".equals(action)||
          "Delete Skill".equals(action)||
          "Move Skill Up".equals(action)||
          "Move Skill Down".equals(action))
          {
          updated=updateEntry(action,
                              session,
                              request,
                              database);
          }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/PositionSkills\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"positionNumber\">");
      String selected="";
      for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
         {
         if(positionNumbers[currentPosition]==positionNumber)
           {
           selected=" SELECTED";
           positionName=positions[currentPosition];
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + positionNumbers[currentPosition] + "\">" + positions[currentPosition]);
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
      if("New Skill".equals(action)||
         "Change Skill".equals(action))
        {
        formScreen(positionNumber,
                   positionName,
                   action,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(positionNumber,
                   positionName,
                   action,
                   updated,
                   session,
                   database,
                   request,
                   response,
                   webPageOutput);
        }
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void viewScreen(int positionNumber,
                           String positionName,
                           String action,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
       {
      int numOfSkills=0;
      int positionSkillNumber=0;
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader(positionName + " Skills",2,webPageOutput);
      boolean skillsFound=false;
      boolean masterSkillsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfPositions=0;
        queryResult=sql.executeQuery("SELECT SkillNumber " +
                                     "FROM skills ");
        if(queryResult.next())
             {
             masterSkillsFound=true;
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find skills entries : " + error,false,context);	
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionSkillNumber,SkillName " +
                                     "FROM positionskills,skills " +
                                     "WHERE PositionNumber=" + positionNumber + " " +
                                     "AND positionskills.SkillNumber=skills.SkillNumber " +
                                     "ORDER BY positionskills.Sequence ASC");
        positionNumber=0;
        String skillName="";
        while(queryResult.next())
             {
             numOfSkills++;
             if(!skillsFound)
               {
               skillsFound=true;
               }
             positionSkillNumber=queryResult.getInt(1);
             skillName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(positionSkillNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + positionSkillNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(skillName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve skills : " + error,false,context);	
        }
      if(!skillsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No skills found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(numOfSkills<21&&masterSkillsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Skill\" NAME=\"action\">");
        }
      if(skillsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Skill\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Skill\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Skill Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Skill Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(int positionNumber,
                              String positionName,
                              String action,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int positionSkillNumber=0;
      int skillNumber=0;
      int skillNumbers[]=null;
      int chosenSkillNumbers[]=null;
      String skillNames[]=null;
      Routines.tableStart(false,webPageOutput);
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfSkills=0;
        queryResult=sql.executeQuery("SELECT SkillNumber,SkillName " +
                                     "FROM skills " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             numOfSkills++;
             }
        skillNames=new String[numOfSkills];
        skillNumbers=new int[numOfSkills];
        if(numOfSkills>0)
          {
          numOfSkills=0;
          queryResult.beforeFirst();
          while(queryResult.next())
               {
               skillNumbers[numOfSkills]=queryResult.getInt(1);
               skillNames[numOfSkills]=queryResult.getString(2);
               numOfSkills++;
               }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find skills entries : " + error,false,context);		
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfSkills=0;
        queryResult=sql.executeQuery("SELECT SkillNumber,PositionSkillNumber " +
                                     "FROM positionskills " +
                                     "WHERE PositionNumber=" + positionNumber + " " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             numOfSkills++;
             }
        chosenSkillNumbers=new int[numOfSkills];
        if(numOfSkills>0)
          {
          numOfSkills=0;
          queryResult.beforeFirst();
          while(queryResult.next())
               {
               chosenSkillNumbers[numOfSkills]=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(queryResult.getInt(2)))))
                 {
                 positionSkillNumber=queryResult.getInt(2);
                 skillNumber=queryResult.getInt(1);
                 }
               numOfSkills++;
               }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find skills entries : " + error,false,context);	
        }
      if("Change Skill".equals(action))
        {
        Routines.tableHeader("Amend details of " + positionName + " Skill",2,webPageOutput);
        }
      if("New Skill".equals(action))
        {
        Routines.tableHeader("Enter details of new " + positionName + " Skill",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Skill");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"skillNumber\">");
      String selectText="";
      for(int currentSkill=0;currentSkill<skillNumbers.length;currentSkill++)
         {
         boolean alreadyChosen=false;
         for(int currentChosenSkill=0;currentChosenSkill<chosenSkillNumbers.length;currentChosenSkill++)
            {
            if(chosenSkillNumbers[currentChosenSkill]==skillNumbers[currentSkill]&&skillNumber!=skillNumbers[currentSkill])
              {
              alreadyChosen=true;
              }
            }
         if(!alreadyChosen)
           {
           if(skillNumbers[currentSkill]==skillNumber)
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + skillNumbers[currentSkill] + "\">" + skillNames[currentSkill]);
           }
        }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Skill".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Skill\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Skill\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"positionNumber\" VALUE=\"" + positionNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"positionSkillNumber\" VALUE=\"" + positionSkillNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int positionSkillNumber=Routines.safeParseInt(request.getParameter("positionSkillNumber"));
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int skillNumber=Routines.safeParseInt(request.getParameter("skillNumber"));
      int sequence=0;
      String skillName="";
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM positionskills " +
                                     "WHERE PositionNumber=" + positionNumber + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(positionSkillNumber==0)
          {
          //Get latest PositionSkillNumber.
          positionSkillNumber=1;
          queryResult=sql.executeQuery("SELECT PositionSkillNumber " +
                                       "FROM positionskills " +
                                       "ORDER BY PositionSkillNumber DESC");
          if(queryResult.first())
            {
            positionSkillNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve positionskills : " + error,false,context);	
        }
      if("Move Skill Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionSkillNumber " +
                                         "FROM positionskills " +
                                         "WHERE PositionNumber=" + positionNumber + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               positionSkillNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(positionSkillNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT positionskills.Sequence,SkillName FROM positionskills,skills " +
                                                "WHERE PositionSkillNumber=" + positionSkillNumber + " " +
                                                "AND positionskills.SkillNumber=skills.SkillNumber");
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the skill list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positionskills " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionNumber=" + positionNumber + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE positionskills " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PositionSkillNumber=" + positionSkillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move skill : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No skills selected");
          }
        success=true;
        }
      if("Move Skill Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionSkillNumber " +
                                         "FROM positionskills " +
                                         "WHERE PositionNumber=" + positionNumber + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               positionSkillNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(positionSkillNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT positionskills.Sequence,SkillName FROM positionskills,skills " +
                                                "WHERE PositionSkillNumber=" + positionSkillNumber + " " +
                                                "AND positionskills.SkillNumber=skills.SkillNumber");
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the skill list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positionskills " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionNumber=" + positionNumber + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE positionskills " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionSkillNumber=" + positionSkillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move positionskills : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No skills selected");
          }
        success=true;
        }
      if("Store New Skill".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionSkillNumber " +
                                       "FROM positionskills " +
                                       "WHERE PositionNumber=" + positionNumber + " " +
                                       "AND SkillNumber=" + skillNumber);
          if(queryResult.first())
            {
            }
          else
            {
            queryResult=sql.executeQuery("SELECT SkillNumber,SkillName " +
                                         "FROM skills " +
                                         "WHERE SkillNumber=" + skillNumber);
            if(queryResult.first())
              {
              skillName=queryResult.getString(2);
              updates=sql.executeUpdate("INSERT INTO positionskills (" +
                                        "PositionSkillNumber,PositionNumber," +
                                        "SkillNumber,Sequence,DateTimeStamp) " +
                                        "VALUES (" +
                                        positionSkillNumber + "," +
                                        positionNumber + "," +
                                        skillNumber + "," +
                                        (sequence+1) + ",'" +
                                        Routines.getDateTime(false) + "')");
              }
            }
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New positionskill not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create positionskills : " + error,false,context);	
          }
        session.setAttribute("message",skillName + " skill stored successfully");
        success=true;
        }
      if("Store Changed Skill".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionSkillNumber " +
                                       "FROM positionskills " +
                                       "WHERE PositionSkillNumber=" + positionSkillNumber);
          if(queryResult.first())
            {
            queryResult=sql.executeQuery("SELECT SkillNumber,SkillName " +
                                         "FROM skills " +
                                         "WHERE SkillNumber=" + skillNumber);
            if(queryResult.first())
              {
              skillName=queryResult.getString(2);
              updates=sql.executeUpdate("UPDATE positionskills " +
                                        "SET SkillNumber=" + skillNumber + "," +
                                        "DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                        "WHERE PositionSkillNumber=" + positionSkillNumber);
              }
            if(updates!=1)
              {
			  Routines.writeToLog(servletName,"Skill not updated, reason unknown",false,context);	
              }
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update positionsskills : " + error,false,context);	
          }
        session.setAttribute("message",skillName + " skill changed successfully");
        success=true;
        }
      if("Delete Skill".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionSkillNumber " +
                                         "FROM positionskills " +
                                         "WHERE PositionNumber=" + positionNumber);
          while(queryResult1.next())
               {
               positionSkillNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(positionSkillNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM positionskills " +
                                            "WHERE PositionSkillNumber=" + positionSkillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not deleted (" + positionSkillNumber + ")",false,context);	
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT PositionSkillNumber " +
                                         "FROM positionskills " +
                                         "WHERE PositionNumber=" + positionNumber + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          positionSkillNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                positionSkillNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE positionskills " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE PositionSkillNumber=" + positionSkillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill entry not reset (" + positionSkillNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete positionskills : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No skills selected");
          }
        success=true;
        }
      return success;
      }
}