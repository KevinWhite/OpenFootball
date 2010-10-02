import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Skills extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Skills";

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
      String action=request.getParameter("action");
      if("New Skill".equals(action)||
         "Change Skill".equals(action))
         {
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
      Routines.WriteHTMLHead("Skills",//title
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
      int skillNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Skill".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SkillNumber " +
                                       "FROM skills ");
          while(queryResult.next())
               {
               skillNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(skillNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find skill entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No skill selected");
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one skill to change");
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
                             "/servlet/Skills\" METHOD=\"POST\">");
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
        formScreen(action,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(action,
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

   private void viewScreen(String action,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      int skillNumber=0;
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Skills",2,webPageOutput);
      boolean skillsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SkillNumber,SkillName " +
                                     "FROM skills " +
                                     "ORDER BY Sequence ASC");
        skillNumber=0;
        String skillName="";
        while(queryResult.next())
             {
             if(!skillsFound)
               {
               skillsFound=true;
               }
             skillNumber=queryResult.getInt(1);
             skillName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(skillNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + skillNumber  + "\" VALUE=\"true\"");
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
        webPageOutput.println("No Skills found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Skill\" NAME=\"action\">");
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

      private void formScreen(String action,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int skillNumber=0;
      int entries=0;
      int minValue=0;
      int maxValue=0;
      String skillName="";
      Routines.tableStart(false,webPageOutput);
      if("Change Skill".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SkillNumber " +
                                       "FROM skills " +
                                       "ORDER BY Sequence DESC");
          int tempSkillNumber=0;
          while(queryResult.next())
               {
               tempSkillNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempSkillNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT SkillNumber,SkillName,Entries,MinValue,MaxValue " +
                                              "FROM skills " +
                                              "WHERE SkillNumber=" + tempSkillNumber);
                 if(queryResult.first())
                   {
                   skillNumber=queryResult.getInt(1);
                   skillName=queryResult.getString(2);
                   entries=queryResult.getInt(3);
                   minValue=queryResult.getInt(4);
                   maxValue=queryResult.getInt(5);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find skill (" + tempSkillNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve skill : " + error,false,context);	
            }
      Routines.tableHeader("Amend details of skill",2,webPageOutput);
      }
      if("New Skill".equals(action))
        {
        Routines.tableHeader("Enter details of new skill",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"skillName\" SIZE=\"20\" MAXLENGTH=\"20\" VALUE=\"" + skillName + "\">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Entries");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"entries\">");
      String selectText="";
      for(int currentEntry=1;currentEntry<13;currentEntry++)
         {
         if(entries==currentEntry||(entries==0&&currentEntry==12))
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentEntry + "\">" + currentEntry);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Minimum Value");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"minValue\">");
      selectText="";
      for(int currentValue=-8;currentValue<100;currentValue++)
         {
         if(minValue==currentValue)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentValue + "\">" + currentValue);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Maximum Value");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"maxValue\">");
      selectText="";
      for(int currentValue=-8;currentValue<100;currentValue++)
         {
         if(maxValue==currentValue)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentValue + "\">" + currentValue);
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
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"skillNumber\" VALUE=\"" + skillNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int skillNumber=Routines.safeParseInt(request.getParameter("skillNumber"));
      int entries=Routines.safeParseInt(request.getParameter("entries"));
      int minValue=Routines.safeParseInt(request.getParameter("minValue"));
      int maxValue=Routines.safeParseInt(request.getParameter("maxValue"));
      if(entries>12)
        {
        entries=12;
        }
      int sequence=0;
      String skillName=request.getParameter("skillName");
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM skills " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(skillNumber==0)
          {
          //Get latest skillNumber.
          skillNumber=1;
          queryResult=sql.executeQuery("SELECT SkillNumber " +
                                       "FROM skills " +
                                       "ORDER BY skillNumber DESC");
          if(queryResult.first())
            {
            skillNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve skills : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT SkillNumber " +
                                         "FROM skills " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               skillNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(skillNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SkillName FROM skills " +
                                                "WHERE SkillNumber=" + skillNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the skill list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE skills " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE skills " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE SkillNumber=" + skillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move skills : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT SkillNumber " +
                                         "FROM skills " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               skillNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(skillNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SkillName FROM skills " +
                                                "WHERE SkillNumber=" + skillNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the skill list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE skills " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE skills " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SkillNumber=" + skillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move skills : " + error,false,context);	
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
          updates=sql.executeUpdate("INSERT INTO skills (" +
                                    "SkillNumber,Sequence,SkillName,Entries,MinValue,MaxValue,DateTimeStamp) " +
                                    "VALUES (" +
                                    skillNumber + "," +
                                    (sequence+1) + ",\"" +
                                    skillName + "\"," +
                                    entries + "," +
                                    minValue + "," +
                                    maxValue + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New skill not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create skill : " + error,false,context);	
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
          updates=sql.executeUpdate("UPDATE skills " +
                                    "SET SkillName='" + skillName + "'," +
                                    "DateTimeStamp='" + Routines.getDateTime(false) + "'," +
                                    "Entries=" + entries + "," +
                                    "MinValue=" + minValue + "," +
                                    "MaxValue=" + maxValue + " " +
                                    "WHERE SkillNumber=" + skillNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Skill not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update skills : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT SkillNumber " +
                                         "FROM skills");
          while(queryResult1.next())
               {
               skillNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(skillNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT skillNumber " +
                                                "FROM positionSkills " +
                                                "WHERE skillNumber=" + skillNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Skill currently in use by positionSkills entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT skillNumber " +
                                                  "FROM skillTables " +
                                                  "WHERE skillNumber=" + skillNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Skill currently in use by skillTables entries");
                     return false;
                     }
                 else
                     {
                     updates=sql2.executeUpdate("DELETE FROM skills " +
                                                "WHERE SkillNumber=" + skillNumber);
                       if(updates!=1)
                         {
						 Routines.writeToLog(servletName,"Skill not deleted (" + skillNumber + ")",false,context);	
                         }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT SkillNumber " +
                                         "FROM skills " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          skillNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                skillNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE skills " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE SkillNumber=" + skillNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Skill entry not reset (" + skillNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete skills : " + error,false,context);	
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