import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class SkillTables extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="SkillTables";

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
      int skillNumber=Routines.safeParseInt(request.getParameter("skillNumber"));
      int tableNumber=Routines.safeParseInt(request.getParameter("tableNumber"));
      String action=request.getParameter("action");
      String[] skills=null;
      int[] skillNumbers=null;
      String disabledText="";
      String skillName="";
      if("New Entry".equals(action)||
         "Change Entry".equals(action))
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
                                "/servlet/SkillTables?jsessionid=" + session.getId());
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
      Routines.WriteHTMLHead("Skill Tables",//title
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
      int skillTableNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
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
             if(skillNumber==0)
               {
               skillNumber=queryResult.getInt(1);
               }
             numOfSkills++;
             }
        skills=new String[numOfSkills];
        skillNumbers=new int[numOfSkills];
        if(numOfSkills>0)
          {
          numOfSkills=0;
          queryResult.beforeFirst();
          while(queryResult.next())
               {
               skillNumbers[numOfSkills]=queryResult.getInt(1);
               skills[numOfSkills]=queryResult.getString(2);
               numOfSkills++;
               }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find skills entries : " + error,false,context);
        }
      if("Change Entry".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SkillTableNumber " +
                                       "FROM skilltables " +
                                       "WHERE SkillNumber=" + skillNumber);
          while(queryResult.next())
               {
               skillTableNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(skillTableNumber))))
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
          Routines.writeToLog(servletName,"Unable to find skilltables entries : " + error,false,context);
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No entry selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one entry to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Entry".equals(action)||
          "Store Changed Entry".equals(action)||
          "Delete Entry".equals(action))
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
                             "/servlet/SkillTables\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"skillNumber\">");
      String selected="";
      for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
         {
         if(skillNumbers[currentSkill]==skillNumber)
           {
           selected=" SELECTED";
           skillName=skills[currentSkill];
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + skillNumbers[currentSkill] + "\">" + skills[currentSkill]);
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
      if("New Entry".equals(action)||
         "Change Entry".equals(action))
        {
        formScreen(skillNumber,
                   skillName,
                   action,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(skillNumber,
                   skillName,
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

   private void viewScreen(int skillNumber,
                           String skillName,
                           String action,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      int numOfSkills=0;
      int skillTableNumber=0;
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader(skillName + " Entries",14,webPageOutput);
      boolean entriesFound=false;
      boolean masterEntriesFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfEntries=0;
        queryResult=sql.executeQuery("SELECT SkillNumber " +
                                     "FROM skilltables ");
        if(queryResult.next())
             {
             masterEntriesFound=true;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find tableskills entries : " + error,false,context);
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SkillTableNumber,Entries,Cost," +
                                     "Value1,Value2,Value3,Value4,Value5,Value6," +
                                     "Value7,Value8,Value9,Value10,Value11,Value12 " +
                                     "FROM skilltables,skills " +
                                     "WHERE skills.SkillNumber=" + skillNumber + " " +
                                     "AND skills.SkillNumber=skilltables.SkillNumber " +
                                     "ORDER BY Cost DESC");
        skillNumber=0;
        int numOfEntries=0;
        while(queryResult.next())
             {
             numOfEntries++;
             if(!entriesFound)
               {
               entriesFound=true;
               Routines.tableDataStart(true,true,true,true,false,0,1,"scoresrow",webPageOutput);
               webPageOutput.println("Sel");
               Routines.tableDataEnd(true,false,false,webPageOutput);
               Routines.tableDataStart(true,true,true,false,false,0,1,"scoresrow",webPageOutput);
               webPageOutput.println("Cost");
               Routines.tableDataEnd(true,false,false,webPageOutput);
               for(int currentTitle=0;currentTitle<12;currentTitle++)
                  {
                  Routines.tableDataEnd(true,false,false,webPageOutput);
                  Routines.tableDataStart(true,true,true,false,false,0,1,"scoresrow",webPageOutput);
                  webPageOutput.println("V"+(currentTitle+1));
                  Routines.tableDataEnd(true,false,false,webPageOutput);
                  }
               Routines.tableDataEnd(true,false,true,webPageOutput);
               }
             skillTableNumber=queryResult.getInt(1);
             int entries=queryResult.getInt(2);
             int cost=queryResult.getInt(3);
             int values[]=new int[entries];
             for(int currentValue=0;currentValue<values.length;currentValue++)
                {
                values[currentValue]=queryResult.getInt(4+currentValue);
                }
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(skillTableNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + skillTableNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,35,0,"scoresrow",webPageOutput);
             webPageOutput.println(cost);
             for(int currentValue=0;currentValue<12;currentValue++)
                {
                Routines.tableDataEnd(false,false,false,webPageOutput);
                Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
                if(currentValue<values.length)
                  {
                  webPageOutput.println(values[currentValue]);
                  }
                else
                  {
                  webPageOutput.println(Routines.indent(1));
                  }
                }
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve skillTables : " + error,false,context);
        }
      if(!entriesFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No entries found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(numOfSkills<101)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Entry\" NAME=\"action\">");
        }
      if(entriesFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Entry\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Entry\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(int skillNumber,
                              String skillName,
                              String action,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int skillTableNumber=0;
      boolean takenCost[]=new boolean[101];
      int cost=0;
      int numOfValues=0;
      int minValue=0;
      int maxValue=0;
      int values[]=null;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Entries,MinValue,MaxValue " +
                                     "FROM skills " +
                                     "WHERE SkillNumber=" + skillNumber);
        if(queryResult.first())
          {
          numOfValues=queryResult.getInt(1);
          minValue=queryResult.getInt(2);
          maxValue=queryResult.getInt(3);
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find skills entry : " + error,false,context);
        }
      values=new int[numOfValues];
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Cost " +
                                     "FROM skilltables " +
                                     "WHERE SkillNumber=" + skillNumber + " " +
                                     "ORDER BY Cost DESC");
        while(queryResult.next())
             {
             cost=queryResult.getInt(1);
             if(takenCost[cost])
               {
               Routines.writeToLog(servletName,"Duplicate cost found (Skill="+skillNumber+",Cost="+cost+")",false,context);
               }
             else
               {
               takenCost[cost]=true;
               }
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find skilltables entries : " + error,false,context);
        }
      cost=0;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SkillTableNumber,Cost," +
                                     "Value1,Value2,Value3,Value4,Value5,Value6," +
                                     "Value7,Value8,Value9,Value10,Value11,Value12 " +
                                     "FROM skilltables " +
                                     "WHERE SkillNumber=" + skillNumber + " " +
                                     "ORDER BY Cost DESC");
        while(queryResult.next())
             {
             if("true".equals(request.getParameter(String.valueOf(queryResult.getInt(1)))))
               {
               skillTableNumber=queryResult.getInt(1);
               cost=queryResult.getInt(2);
               for(int currentValue=0;currentValue<values.length;currentValue++)
                  {
                  values[currentValue]=queryResult.getInt(3+currentValue);
                  }
               }
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find skilltables entries : " + error,false,context);
        }
      Routines.tableStart(false,webPageOutput);
      if("Change Entry".equals(action))
        {
        Routines.tableHeader("Amend details of " + skillName + " Table",2,webPageOutput);
        }
      if("New Entry".equals(action))
        {
        Routines.tableHeader("Enter details of new " + skillName + " Table",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Cost");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"cost\">");
      String selectText="";
        for(int currentCost=0;currentCost<takenCost.length;currentCost++)
           {
           if(!takenCost[currentCost]||currentCost==cost)
             {
             if(currentCost==cost)
               {
               selectText=" SELECTED";
               }
             else
               {
               selectText="";
               }
             webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentCost + "\">" + currentCost);
             }
        }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      for(int currentValue=0;currentValue<values.length;currentValue++)
         {
         Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
         webPageOutput.print("Value"+(currentValue+1));
         Routines.tableDataEnd(false,false,false,webPageOutput);
         Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
         webPageOutput.println("<SELECT NAME=\"value"+(currentValue+1)+"\">");
         selectText="";
         for(int currentValue2=minValue;currentValue2<=maxValue;currentValue2++)
           {
           if(currentValue2==values[currentValue])
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentValue2 + "\">" + currentValue2);
           }
         webPageOutput.println("</SELECT>");
         Routines.tableDataEnd(false,false,true,webPageOutput);
         }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Entry".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Entry\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Entry\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"skillNumber\" VALUE=\"" + skillNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"skillTableNumber\" VALUE=\"" + skillTableNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      String skillName="";
      int skillNumber=Routines.safeParseInt(request.getParameter("skillNumber"));
      int skillTableNumber=Routines.safeParseInt(request.getParameter("skillTableNumber"));
      int cost=Routines.safeParseInt(request.getParameter("cost"));
      int values[]=new int[12];
      for(int currentValue=0;currentValue<values.length;currentValue++)
         {
         values[currentValue]=Routines.safeParseInt(request.getParameter("value"+(currentValue+1)));
         }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        if(skillTableNumber==0)
          {
          //Get latest SkillTableNumber.
          skillTableNumber=1;
          queryResult=sql.executeQuery("SELECT SkillTableNumber " +
                                       "FROM skilltables " +
                                       "ORDER BY SkillTableNumber DESC");
          if(queryResult.first())
            {
            skillTableNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve skillTables : " + error,false,context);
        }
      if("Store New Entry".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SkillTableNumber " +
                                       "FROM skilltables " +
                                       "WHERE SkillNumber=" + skillNumber + " " +
                                       "AND Cost=" + cost);
          if(queryResult.first())
            {
            Routines.writeToLog(servletName,"skilltables entry already exists (SkillNumber="+skillNumber+",Cost="+cost,false,context);
            }
          else
            {
            queryResult=sql.executeQuery("SELECT SkillName " +
                                         "FROM skills " +
                                         "WHERE SkillNumber=" + skillNumber);
            if(queryResult.first())
              {
              skillName=queryResult.getString(1);
              updates=sql.executeUpdate("INSERT INTO skilltables (" +
                                        "SkillTableNumber,SkillNumber,Cost," +
                                        "Value1,Value2,Value3,Value4,Value5,Value6," +
                                        "Value7,Value8,Value9,Value10,Value11,Value12," +
                                        "DateTimeStamp) " +
                                        "VALUES (" +
                                        skillTableNumber + "," +
                                        skillNumber + "," +
                                        cost + "," +
                                        values[0] + "," +
                                        values[1] + "," +
                                        values[2] + "," +
                                        values[3] + "," +
                                        values[4] + "," +
                                        values[5] + "," +
                                        values[6] + "," +
                                        values[7] + "," +
                                        values[8] + "," +
                                        values[9] + "," +
                                        values[10] + "," +
                                        values[11] + ",'" +
                                        Routines.getDateTime(false) + "')");
              }
            }
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"New skilltable not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create skilltables : " + error,false,context);
          }
        session.setAttribute("message",skillName + " entry stored successfully");
        success=true;
        }
      if("Store Changed Entry".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SkillTableNumber " +
                                       "FROM skilltables " +
                                       "WHERE SkillTableNumber=" + skillTableNumber);
          if(queryResult.first())
            {
            queryResult=sql.executeQuery("SELECT SkillTableNumber " +
                                         "FROM skilltables " +
                                         "WHERE SkillNumber=" + skillNumber + " " +
                                         "AND Cost=" + cost);
            if(queryResult.first()&&queryResult.getInt(1)!=skillTableNumber)
              {
              Routines.writeToLog(servletName,"skilltables entry already exists (SkillNumber="+skillNumber+",Cost="+cost,false,context);
              }
            else
              {
              queryResult=sql.executeQuery("SELECT SkillName " +
                                           "FROM skills " +
                                           "WHERE SkillNumber=" + skillNumber);
              if(queryResult.first())
                {
                skillName=queryResult.getString(1);
                updates=sql.executeUpdate("UPDATE skilltables " +
                                          "SET Cost=" + cost + "," +
                                          "Value1=" + values[0] + "," +
                                          "Value2=" + values[1] + "," +
                                          "Value3=" + values[2] + "," +
                                          "Value4=" + values[3] + "," +
                                          "Value5=" + values[4] + "," +
                                          "Value6=" + values[5] + "," +
                                          "Value7=" + values[6] + "," +
                                          "Value8=" + values[7] + "," +
                                          "Value9=" + values[8] + "," +
                                          "Value10=" + values[9] + "," +
                                          "Value11=" + values[10] + "," +
                                          "Value12=" + values[11] + "," +
                                          "DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                          "WHERE SkillTableNumber=" + skillTableNumber);
                }
              }
            if(updates!=1)
              {
              Routines.writeToLog(servletName,"SkillTable not updated, reason unknown",false,context);
              }
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update skilltables : ",false,context);
          }
        session.setAttribute("message",skillName + " entry changed successfully");
        success=true;
        }
      if("Delete Entry".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SkillTableNumber " +
                                         "FROM skilltables " +
                                         "WHERE SkillNumber=" + skillNumber);
          while(queryResult1.next())
               {
               skillTableNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(skillTableNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM skilltables " +
                                            "WHERE SkillTableNumber=" + skillTableNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Entry not deleted (" + skillTableNumber + ")",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete skilltables : " + error,false,context);
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No entry selected");
          }
        success=true;
        }
      return success;
      }
}