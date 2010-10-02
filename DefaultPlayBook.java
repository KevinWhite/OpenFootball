import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DefaultPlayBook extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="DefaultPlayBook";

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
      boolean teLeft=false;
      boolean teRight=false;
      boolean back1=false;
      boolean back2=false;
      boolean back3=false;
      int situationNumber=Routines.safeParseInt(request.getParameter("situationNumber"));
      int playBookNumber=Routines.safeParseInt(request.getParameter("playBookNumber"));
      int type=Routines.safeParseInt(request.getParameter("type"));
      String action=request.getParameter("action");
      String[] types={"Offense","Defense","Special Teams"};
      int maxSetNumber=0;
      int maxFormationNumber=0;
      int maxPlayNumber=0;
      int maxPrimaryStrategyNumber=0;
      int maxSecondaryStrategyNumber=0;
      int[] setNumbers=new int[100];
      int[] formationNumbers=new int[100];
      int[] playNumbers=new int[100];
      int[] playTypes=new int[100];
      int[] primaryStrategyNumbers=new int[100];
      int[] secondaryStrategyNumbers=new int[100];
      int storePlayBookNumber=0;
      int storeSetNumber=0;
      int storeFormationNumber=0;
      int storePositionNumber=0;
      int storePlayNumber=0;
      int storePlayType=0;
      int storePrimaryStrategyNumber=0;
      int specialTeamsType=0;
      int[] storeSecondaryStrategyNumber=new int[10];
      String situationName="";
      String[] setNames=new String[100];
      String[] formationNames=new String[100];
      String[] formationSides=new String[100];
      String[] playNames=new String[100];
      String[] primaryStrategyNames=new String[100];
      String[] secondaryStrategyNames=new String[100];
      String[] typeOptions=new String[10];
      String disabledText="";
      if(type==0||type==1)
        {
        typeOptions[0]="Run";
        typeOptions[1]="Pass";
        }
      if("New Entry".equals(action)||
         "Change Entry".equals(action)||
         "Store Formation".equals(action)||
         "Update Formation".equals(action)||
         "Store Player".equals(action)||
         "Update Player".equals(action)||
         ("Store Play".equals(action)&&type!=2)||
         ("Update Play".equals(action)&&type!=2))
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
      Routines.WriteHTMLHead("Default PlayBook",//title
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
      //Load Sets
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SetNumber,SetName " +
                                     "FROM defaultsets " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             setNumbers[maxSetNumber]=queryResult.getInt(1);
             setNames[maxSetNumber]=queryResult.getString(2);
             maxSetNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find defaultset entries : " + error,false,context);
        }
      //Load Formations
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT FormationNumber,FormationName,Position2,Position8 " +
                                     "FROM formations " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        int leftEnd=0;
        int rightEnd=0;
        while(queryResult.next())
             {
             formationNumbers[maxFormationNumber]=queryResult.getInt(1);
             formationNames[maxFormationNumber]=queryResult.getString(2);
             leftEnd=queryResult.getInt(3);
             rightEnd=queryResult.getInt(4);
             formationSides[maxFormationNumber]="";
             if(leftEnd!=0&&rightEnd==0)
               {
               formationSides[maxFormationNumber]=" (Left)";
               }
             if(leftEnd==0&&rightEnd!=0)
               {
               formationSides[maxFormationNumber]=" (Right)";
               }
             maxFormationNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find formation entries : " + error,false,context);
        }
      //Load Plays
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PlayNumber,PlayName,SubType " +
                                     "FROM plays " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             playNumbers[maxPlayNumber]=queryResult.getInt(1);
             playNames[maxPlayNumber]=queryResult.getString(2);
             playTypes[maxPlayNumber]=queryResult.getInt(3);
             maxPlayNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find plays entries : " + error,false,context);
        }
      //Load Primary Strategies
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT StrategyNumber,StrategyName " +
                                     "FROM strategies " +
                                     "WHERE Type=" + type + " " +
                                     "AND SubType=1 " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             primaryStrategyNumbers[maxPrimaryStrategyNumber]=queryResult.getInt(1);
             primaryStrategyNames[maxPrimaryStrategyNumber]=queryResult.getString(2);
             maxPrimaryStrategyNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find primary strategy entries : " + error,false,context);
        }
      //Load Secondary Strategies
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT StrategyNumber,StrategyName " +
                                     "FROM strategies " +
                                     "WHERE Type=" + type + " " +
                                     "AND SubType=2 " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             secondaryStrategyNumbers[maxSecondaryStrategyNumber]=queryResult.getInt(1);
             secondaryStrategyNames[maxSecondaryStrategyNumber]=queryResult.getString(2);
             maxSecondaryStrategyNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find secondary strategy entries : " + error,false,context);
        }
      if("Change Entry".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayBookNumber " +
                                       "FROM defaultplaybook " +
                                       "WHERE SituationNumber=" + situationNumber);
          while(queryResult.next())
               {
               playBookNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
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
          Routines.writeToLog(servletName,"Unable to find defaultplaybook entries : " + error,false,context);
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
      if ("Store Strats".equals(action)||
          "Update Strats".equals(action)||
          ("Store Play".equals(action)&&type==2)||
          ("Update Play".equals(action)&&type==2)||
          "Delete Entry".equals(action)||
          "Move Entry Up".equals(action)||
          "Move Entry Down".equals(action))
          {
          updated=updateEntry(type,
                              action,
                              maxSecondaryStrategyNumber,
                              secondaryStrategyNumbers,
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
                             "/servlet/DefaultPlayBook\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Situation",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"situationNumber\">");
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SituationNumber,SituationName,SituationCallNumber1 " +
                                     "FROM defaultsituations " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             String selectedSituation="";
             int tempSituationNumber=queryResult.getInt(1);
             if(situationNumber==0)
               {
               situationNumber=tempSituationNumber;
               situationName=queryResult.getString(2);
               }
             String tempSituationName=queryResult.getString(2);
             if(situationNumber==tempSituationNumber)
               {
               selectedSituation=" SELECTED";
               situationName=queryResult.getString(2);
               specialTeamsType=queryResult.getInt(3);
               }
             else
               {
               selectedSituation="";
               }
             webPageOutput.println(" <OPTION" + disabledText + selectedSituation + " VALUE=\"" + tempSituationNumber + "\">" + tempSituationName);
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find defaultsituations entries : " + error,false,context);
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
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      Routines.tableHeader("Plays to be called for " + situationName + " situation",19,webPageOutput);
      boolean playBookFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PlayBookNumber," +
                                     "defaultplaybook.SetNumber,defaultplaybook.FormationNumber,defaultplaybook.PositionNumber," +
                                     "defaultplaybook.PlayNumber,PrimaryStrategyNumber," +
                                     "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                     "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                     "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                     "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                     "SecondaryStrategyNumber9,SecondaryStrategyNumber10," +
                                     "SetName,FormationName,PlayName,StrategyName,plays.SubType," +
                                     "Position1,Position2,Position3,Position4,Position5," +
                                     "Position6,Position7,Position8,Position9,Position10," +
                                     "Position11,Position12,Position13,Position14,Position15 " +
                                     "FROM defaultplaybook,defaultsituations,defaultsets,formations,plays,strategies " +
                                     "WHERE defaultplaybook.SituationNumber=" + situationNumber  + " " +
                                     "AND defaultplaybook.SituationNumber=defaultsituations.SituationNumber " +
                                     "AND defaultsituations.Type=" + type + " " +
                                     "AND defaultplaybook.SetNumber=defaultsets.SetNumber " +
                                     "AND defaultplaybook.FormationNumber=formations.formationNumber " +
                                     "AND defaultplaybook.PlayNumber=plays.PlayNumber " +
                                     "AND defaultplaybook.PrimaryStrategyNumber=strategies.StrategyNumber " +
                                     "ORDER BY defaultplaybook.Sequence ASC");
        int currentPlayBookNumber=0;
        int currentSetNumber=0;
        int currentFormationNumber=0;
        int currentPositionNumber=0;
        int currentPlayNumber=0;
        int currentPrimaryStrategyNumber=0;
        int[] currentSecondaryStrategyNumber=new int[10];
        String currentSetName="";
        String currentFormationName="";
        String currentPositionName="";
        String currentPlayName="";
        String currentPrimaryStrategyName="";
        int currentPlayType=0;
        int[] positions=new int[15];
        while(queryResult.next())
             {
             if(!playBookFound)
               {
               playBookFound=true;
               tableHeaders(type,webPageOutput);
               }
             currentPlayBookNumber=queryResult.getInt(1);
             currentSetNumber=queryResult.getInt(2);
             currentFormationNumber=queryResult.getInt(3);
             currentPositionNumber=queryResult.getInt(4);
             currentPlayNumber=queryResult.getInt(5);
             currentPrimaryStrategyNumber=queryResult.getInt(6);
             for(int currentStrategy=0;currentStrategy<10;currentStrategy++)
                {
                currentSecondaryStrategyNumber[currentStrategy]=queryResult.getInt(7+currentStrategy);
                }
             currentSetName=queryResult.getString(17);
             currentFormationName=queryResult.getString(18);
             currentPlayName=queryResult.getString(19);
             currentPrimaryStrategyName=queryResult.getString(20);
             currentPlayType=queryResult.getInt(21);
             int maxNumOfRB=0;
             int maxNumOfFB=0;
             int maxNumOfWR=0;
             int maxNumOfTE=0;
             for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
                {
                positions[currentPosition]=queryResult.getInt(22+currentPosition);
                if(positions[currentPosition]==15)
                  {
                  maxNumOfRB++;
                  }
                if(positions[currentPosition]==16)
                  {
                  maxNumOfFB++;
                  }
                if(positions[currentPosition]==13)
                  {
                  maxNumOfWR++;
                  }
                if(positions[currentPosition]==12)
                  {
                  maxNumOfTE++;
                  }
                }
             if(positions[1]==12)
               {
               teLeft=true;
               }
             else
               {
               teLeft=false;
               }
             if(positions[7]==12)
               {
               teRight=true;
               }
             else
               {
               teRight=false;
               }
             if(positions[12]==15||positions[12]==16)
               {
               back1=true;
               }
             if(positions[13]==15||positions[13]==16)
               {
               back2=true;
               }
             if(positions[14]==15||positions[14]==16)
               {
               back3=true;
               }
             Statement sql2=database.createStatement();
             ResultSet queryResult2;
             int realPositionNumber=0;
             String positionText="";
             int numOfRB=0;
             int numOfFB=0;
             int numOfWR=0;
             int numOfTE=0;
             for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
                {
                if(positions[currentPosition]==15)
                  {
                  numOfRB++;
                  }
                if(positions[currentPosition]==16)
                  {
                  numOfFB++;
                  }
                if(positions[currentPosition]==13)
                  {
                  numOfWR++;
                  }
                if(positions[currentPosition]==12)
                  {
                  numOfTE++;
                  }
                if(currentPositionNumber==currentPosition+1)
                  {
                  realPositionNumber=positions[currentPosition];
                  if(realPositionNumber==15&&maxNumOfRB>1)
                    {
                    positionText=String.valueOf(numOfRB);
                    }
                  if(realPositionNumber==16&&maxNumOfFB>1)
                    {
                    positionText=String.valueOf(numOfFB);
                    }
                  if(realPositionNumber==13&&maxNumOfWR>1)
                    {
                    positionText=String.valueOf(numOfWR);
                    }
                  if(realPositionNumber==12&&maxNumOfTE>1)
                    {
                    positionText=String.valueOf(numOfTE);
                    }
                  }
                }
             String[] backText=new String[3];
             int rbCount=1;
             int fbCount=1;
             for(int currentBack=0;currentBack<backText.length;currentBack++)
                {
                if(positions[12+currentBack]==15)
                  {
                  if(numOfRB>1)
                    {
                    backText[currentBack]="RB"+rbCount+" Blocking";
                    rbCount++;
                    }
                  else
                    {
                    backText[currentBack]="RB Blocking";
                    }
                  }
                if(positions[12+currentBack]==16)
                  {
                  if(numOfFB>1)
                    {
                    backText[currentBack]="FB"+fbCount+" Blocking";
                    fbCount++;
                    }
                  else
                    {
                    backText[currentBack]="FB Blocking";
                    }
                  }
                }
             queryResult2=sql2.executeQuery("SELECT PositionCode " +
                                            "FROM positions " +
                                            "WHERE PositionNumber=" +
                                            realPositionNumber);
             queryResult2.first();
             currentPositionName=queryResult2.getString(1);
             boolean bold=false;
             if(("Change Entry".equals(action)||"Update Formation".equals(action)||"Update Player".equals(action)||"Update Play".equals(action))&&
               "true".equals(request.getParameter(String.valueOf(currentPlayBookNumber))))
               {
               bold=true;
               storePlayBookNumber=queryResult.getInt(1);;
               storeSetNumber=queryResult.getInt(2);
               storeFormationNumber=queryResult.getInt(3);
               storePositionNumber=queryResult.getInt(4);
               storePlayNumber=queryResult.getInt(5);
               storePrimaryStrategyNumber=queryResult.getInt(6);
               for(int currentStrategy=0;currentStrategy<10;currentStrategy++)
                  {
                  storeSecondaryStrategyNumber[currentStrategy]=queryResult.getInt(7+currentStrategy);
                  }
               storePlayType=queryResult.getInt(21);
               }
             Routines.tableDataStart(true,false,bold,true,false,3,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated||"Change Entry".equals(action)||"Update Formation".equals(action)||"Update Player".equals(action)||"Update Play".equals(action))
               {
               param=request.getParameter(String.valueOf(currentPlayBookNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             if("New Entry".equals(action)||
                "Change Entry".equals(action)||
                "Store Formation".equals(action)||
                "Update Formation".equals(action)||
                "Store Player".equals(action)||
                "Update Player".equals(action)||
                ("Store Play".equals(action)&&type!=2)||
                ("Update Play".equals(action)&&type!=2))
                {
                disabledText=" DISABLED";
                if(selected)
                  {
                  webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"" + currentPlayBookNumber + "\" VALUE=\"true\">");
                  }
                }
             else
                {
                disabledText="";
                }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\"" + disabledText + " NAME=\"" + currentPlayBookNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,10,0,"scoresrow",webPageOutput);
             webPageOutput.println(currentSetName);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,true,bold,false,false,16,0,"scoresrow",webPageOutput);
             webPageOutput.println(currentFormationName);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             if(type==0)
               {
               Routines.tableDataStart(true,true,bold,false,false,6,0,"scoresrow",webPageOutput);
               String strongText="";
               if(positions[1]!=0&&positions[7]==0)
                 {
                 strongText="Left";
                 }
               if(positions[1]==0&&positions[7]!=0)
                 {
                 strongText="Right";
                 }
               webPageOutput.println(strongText);
               Routines.tableDataEnd(bold,false,false,webPageOutput);
               }
             if(type!=2)
               {
               Routines.tableDataStart(true,true,bold,false,false,6,0,"scoresrow",webPageOutput);
               webPageOutput.println(typeOptions[currentPlayType]);
               Routines.tableDataEnd(bold,false,false,webPageOutput);
               }
             Routines.tableDataStart(true,true,bold,false,false,16,0,"scoresrow",webPageOutput);
             webPageOutput.println(currentPlayName);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             if(type==0)
               {
               Routines.tableDataStart(true,true,bold,false,false,10,0,"scoresrow",webPageOutput);
               webPageOutput.println(currentPositionName+positionText);
               Routines.tableDataEnd(bold,false,false,webPageOutput);
               }
             if(type!=2)
               {
               Routines.tableDataStart(true,true,bold,false,false,16,0,"scoresrow",webPageOutput);
               webPageOutput.println(currentPrimaryStrategyName);
               Routines.tableDataEnd(bold,false,false,webPageOutput);
               Routines.tableDataStart(true,false,bold,false,false,16,0,"scoresrow",webPageOutput);
               if(currentSecondaryStrategyNumber[0]==0)
                 {
                 webPageOutput.println("None");
                 }
               else
                 {
                 webPageOutput.println("<SELECT" + disabledText + " NAME=\"dummy\">");
                 for(int currentStrategy=0;currentStrategy<currentSecondaryStrategyNumber.length;currentStrategy++)
                    {
                    if(currentSecondaryStrategyNumber[currentStrategy]!=0)
                      {
                      for(int currentOption=0;currentOption<maxSecondaryStrategyNumber;currentOption++)
                         {
                         if(currentSecondaryStrategyNumber[currentStrategy]==secondaryStrategyNumbers[currentOption])
                           {
                           String tempText=secondaryStrategyNames[currentOption];
                           if(currentSecondaryStrategyNumber[currentStrategy]==10)
                             {
                             if(teRight)
                               {
                               tempText="TE1 Blocking";
                               }
                             else
                               {
                               tempText="TE Blocking";
                               }
                             }
                           if(currentSecondaryStrategyNumber[currentStrategy]==11)
                             {
                             if(teLeft)
                               {
                               tempText="TE2 Blocking";
                               }
                             else
                               {
                               tempText="TE Blocking";
                               }
                             }
                           if(currentSecondaryStrategyNumber[currentStrategy]==12)
                             {
                             tempText=backText[0];
                             }
                           if(currentSecondaryStrategyNumber[currentStrategy]==13)
                             {
                             tempText=backText[1];
                             }
                           if(currentSecondaryStrategyNumber[currentStrategy]==14)
                             {
                             tempText=backText[2];
                             }
                           webPageOutput.println(" <OPTION VALUE=\"dummy\">" + tempText);
                           }
                         }
                      }
                    }
                 webPageOutput.println("</SELECT>");
                 }
               Routines.tableDataEnd(bold,false,true,webPageOutput);
               }
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve defaultplaybook entries : " + error,false,context);
        }
      if(!playBookFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No PlayBook entries found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      if("New Entry".equals(action))
        {
        storePlayBookNumber=0;
        storeSetNumber=0;
        storeFormationNumber=7;
        storePositionNumber=0;
        storePlayNumber=0;
        storePlayType=0;
        storePrimaryStrategyNumber=0;
        storeSecondaryStrategyNumber=null;
        }
      if(type==2)
        {
        storePlayType=specialTeamsType;
        }
      if("New Entry".equals(action)||
         "Change Entry".equals(action)||
         "Store Formation".equals(action)||
         "Update Formation".equals(action)||
         "Store Player".equals(action)||
         "Update Player".equals(action)||
         ("Store Play".equals(action)&&type!=2)||
         ("Update Play".equals(action)&&type!=2))
        {
        formLine(action,
                 titleText,
                 type,
                 storeSetNumber,
                 storeFormationNumber,
                 storePositionNumber,
                 storePlayNumber,
                 storePlayType,
                 storePrimaryStrategyNumber,
                 storeSecondaryStrategyNumber,
                 maxSetNumber,
                 maxFormationNumber,
                 maxPlayNumber,
                 maxPrimaryStrategyNumber,
                 maxSecondaryStrategyNumber,
                 setNumbers,
                 formationNumbers,
                 playNumbers,
                 playTypes,
                 primaryStrategyNumbers,
                 secondaryStrategyNumbers,
                 setNames,
                 formationNames,
                 formationSides,
                 playNames,
                 primaryStrategyNames,
                 secondaryStrategyNames,
                 typeOptions,
                 request,
                 webPageOutput,
                 database);
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if("New Entry".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Player\" NAME=\"action\">");
          }
        if(type!=2)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Strats\" NAME=\"action\">");
          }
        }
      if("Change Entry".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Update Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Player\" NAME=\"action\">");
          }
        if(type!=2)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Strats\" NAME=\"action\">");
          }
        }
      if("Store Formation".equals(action))
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Player\" NAME=\"action\">");
          }
        if(type!=2)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Strats\" NAME=\"action\">");
          }
        }
      if("Update Formation".equals(action))
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Update Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Player\" NAME=\"action\">");
          }
        if(type!=2)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Strats\" NAME=\"action\">");
          }
        }
      if("Store Play".equals(action)&&type==0)
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Play\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Player\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Strats\" NAME=\"action\">");
        }
      if("Update Play".equals(action)&&type==0)
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Play\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Update Player\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Strats\" NAME=\"action\">");
        }
      if("Store Player".equals(action)||("Store Play".equals(action)&&type==1))
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Store Player\" NAME=\"action\">");
          }
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Strats\" NAME=\"action\">");
        }
      if("Update Player".equals(action)||("Update Play".equals(action)&&type==1))
        {
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Play\" NAME=\"action\">");
        if(type==0)
          {
          webPageOutput.println("<INPUT DISABLED TYPE=\"SUBMIT\" VALUE=\"Update Player\" NAME=\"action\">");
          }
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Update Strats\" NAME=\"action\">");
        }
      if("New Entry".equals(action)||
         "Change Entry".equals(action)||
         "Store Formation".equals(action)||
         "Update Formation".equals(action)||
         "Store Player".equals(action)||
         "Update Player".equals(action)||
         ("Store Play".equals(action)&&type!=2)||
         ("Update Play".equals(action)&&type!=2))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"situationNumber\" VALUE=\"" + situationNumber + "\">");
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playBookNumber\" VALUE=\"" + storePlayBookNumber + "\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Entry\" NAME=\"action\">");
        if(playBookFound)
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Entry\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Entry\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Entry Up\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Entry Down\" NAME=\"action\">");
          }
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"type\" VALUE=\"" + type + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void tableHeaders(int type,
                             PrintWriter webPageOutput)
      {
      //Title Line 1
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("Set");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,true,true,false,false,16,0,"scoresrow",webPageOutput);
      webPageOutput.println("Formation");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      if(type==0)
        {
        Routines.tableDataStart(true,true,true,false,false,6,0,"scoresrow",webPageOutput);
        webPageOutput.println("Strong");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        }
      if(type!=2)
        {
        Routines.tableDataStart(true,false,true,false,false,6,0,"scoresrow",webPageOutput);
        webPageOutput.println("Type");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        }
      Routines.tableDataStart(true,false,true,false,false,16,0,"scoresrow",webPageOutput);
      webPageOutput.println("Play");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      if(type==0)
        {
        Routines.tableDataStart(true,false,true,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("Player");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        }
      if(type!=2)
        {
        Routines.tableDataStart(true,false,true,false,false,16,0,"scoresrow",webPageOutput);
        webPageOutput.println("Primary Strategy");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        Routines.tableDataStart(true,false,true,false,false,16,0,"scoresrow",webPageOutput);
        webPageOutput.println("Secondary Strategy");
        Routines.tableDataEnd(true,false,true,webPageOutput);
        }
      }

   private void formLine(String action,
                         String titleText,
                         int type,
                         int setNumber,
                         int formationNumber,
                         int positionNumber,
                         int playNumber,
                         int playType,
                         int primaryStrategyNumber,
                         int[] secondaryStrategyNumber,
                         int maxSetNumber,
                         int maxFormationNumber,
                         int maxPlayNumber,
                         int maxPrimaryStrategyNumber,
                         int maxSecondaryStrategyNumber,
                         int[] setNumbers,
                         int[] formationNumbers,
                         int[] playNumbers,
                         int[] playTypes,
                         int[] primaryStrategyNumbers,
                         int[] secondaryStrategyNumbers,
                         String[] setNames,
                         String[] formationNames,
                         String[] formationSides,
                         String[] playNames,
                         String[] primaryStrategyNames,
                         String[] secondaryStrategyNames,
                         String[] typeOptions,
                         HttpServletRequest request,
                         PrintWriter webPageOutput,
                         Connection  database)
      {
      boolean teLeft=false;
      boolean teRight=false;
      boolean back1=false;
      boolean back2=false;
      boolean back3=false;
      String selectText="";
      String disabledText="";
      String ballCarriers[]=new String[6];
      int ballCarrierNumbers[]=new int[6];
      int realPositionNumbers[]=new int[6];
      int realPositionNumber=0;
      int tempFormationNumber=formationNumber;
      boolean shotGun=false;
      boolean allowPitch=false;
      boolean allowDouble=false;
      boolean allowTriple=false;
      boolean allowStunt=false;
      boolean allowOLLB=false;
      boolean allowILLB=false;
      boolean allowMLB=false;
      boolean allowIRLB=false;
      boolean allowORLB=false;
      boolean allowLCB=false;
      boolean allowNB=false;
      boolean allowDB=false;
      boolean allowFS=false;
      boolean allowSS=false;
      boolean allowRCB=false;
      Routines.tableStart(false,webPageOutput);
      if("New Entry".equals(action)||"Store Formation".equals(action)||"Store Player".equals(action)||"Store Play".equals(action))
        {
        Routines.tableHeader("Enter details of Store " + titleText + " PlayBook entry",18,webPageOutput);
        }
      if("Change Entry".equals(action)||"Update Formation".equals(action)||"Update Player".equals(action)||"Update Play".equals(action))
        {
        Routines.tableHeader("Amend details of " + titleText + " PlayBook entry",18,webPageOutput);
        }
      //Title Line 1
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,17,0,"scoresrow",webPageOutput);
      webPageOutput.println("Set");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,true,true,false,false,17,0,"scoresrow",webPageOutput);
      webPageOutput.println("Formation");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      if(type!=2)
        {
        Routines.tableDataStart(true,true,true,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("Play Type");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        }
      Routines.tableDataStart(true,false,true,false,false,17,0,"scoresrow",webPageOutput);
      webPageOutput.println("Play");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      if(type==0)
        {
        Routines.tableDataStart(true,false,true,false,false,17,0,"scoresrow",webPageOutput);
        webPageOutput.println("Player");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        }
      if(type!=2)
        {
        Routines.tableDataStart(true,false,true,false,false,17,0,"scoresrow",webPageOutput);
        webPageOutput.println("Primary Strategy");
        Routines.tableDataEnd(true,false,true,webPageOutput);
        }
      Routines.tableDataStart(true,true,false,true,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" DISABLED NAME=\"dummy\" VALUE=\"true\" CHECKED>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,17,0,"scoresrow",webPageOutput);
      if("New Entry".equals(action)||"Change Entry".equals(action))
        {
        webPageOutput.println("<SELECT NAME=\"setNumber\">");
        for(int currentSet=0;currentSet<maxSetNumber;currentSet++)
           {
           if(setNumber==setNumbers[currentSet])
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + setNumbers[currentSet] + "\">" + setNames[currentSet]);
           }
         webPageOutput.println("</SELECT>");
         }
      else
         {
         webPageOutput.println("<SELECT DISABLED NAME=\"setNumber\">");
         setNumber=Routines.safeParseInt(request.getParameter("setNumber"));
         for(int currentSet=0;currentSet<maxSetNumber;currentSet++)
            {
            if(setNumber==setNumbers[currentSet])
              {
              webPageOutput.println(" <OPTION SELECTED VALUE=\"" + setNumbers[currentSet] + "\">" + setNames[currentSet]);
              webPageOutput.println("</SELECT>");
              webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"setNumber\" VALUE=\"" + setNumber + "\">");
              }
            }
          }
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,true,false,false,false,17,0,"scoresrow",webPageOutput);
      if("New Entry".equals(action)||"Change Entry".equals(action))
        {
        webPageOutput.println("<SELECT NAME=\"formationNumber\">");
        for(int currentFormation=0;currentFormation<maxFormationNumber;currentFormation++)
           {
           if(type==2&&playType==15&&(formationNumbers[currentFormation]!=33&&formationNumbers[currentFormation]!=35)||
              type==2&&playType==16&&(formationNumbers[currentFormation]!=34)||
              type==2&&playType==17&&(formationNumbers[currentFormation]!=37&&formationNumbers[currentFormation]!=38&&formationNumbers[currentFormation]!=39)||
              type==2&&playType==18&&(formationNumbers[currentFormation]!=36))
             {
             }
           else
             {
             if(formationNumber==formationNumbers[currentFormation])
               {
               selectText=" SELECTED";
               }
             else
               {
               selectText="";
               }
             if(type==0)
               {
               webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + formationNumbers[currentFormation] + "\">" + formationNames[currentFormation] + formationSides[currentFormation]);
               }
             else
               {
               webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + formationNumbers[currentFormation] + "\">" + formationNames[currentFormation]);
               }
             }
           }
        webPageOutput.println("</SELECT>");
        }
      else
         {
         webPageOutput.println("<SELECT DISABLED NAME=\"formationNumber\">");
         formationNumber=Routines.safeParseInt(request.getParameter("formationNumber"));
         for(int currentFormation=0;currentFormation<maxFormationNumber;currentFormation++)
            {
            if(formationNumber==formationNumbers[currentFormation])
              {
              if(type==0)
                {
                webPageOutput.println(" <OPTION SELECTED VALUE=\"" + formationNumbers[currentFormation] + "\">" + formationNames[currentFormation] + formationSides[currentFormation]);
                }
              else
                {
                webPageOutput.println(" <OPTION SELECTED VALUE=\"" + formationNumbers[currentFormation] + "\">" + formationNames[currentFormation]);
                }
              webPageOutput.println("</SELECT>");
              webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"formationNumber\" VALUE=\"" + formationNumber + "\">");
              }
            }
          }
      Routines.tableDataEnd(false,false,false,webPageOutput);
      if(type!=2)
        {
        Routines.tableDataStart(true,true,false,false,false,10,0,"scoresrow",webPageOutput);
        if("New Entry".equals(action)||"Change Entry".equals(action))
          {
          webPageOutput.println("<SELECT NAME=\"playType\">");
          for(int currentType=0;currentType<typeOptions.length;currentType++)
             {
             if(typeOptions[currentType]==null)
               {
               }
             else
               {
               if(playType==currentType)
                 {
                 selectText=" SELECTED";
                 }
               else
                 {
                 selectText="";
                 }
               webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentType + "\">" + typeOptions[currentType]);
               }
             }
          webPageOutput.println("</SELECT>");
          }
        else
           {
           webPageOutput.println("<SELECT DISABLED NAME=\"playType\">");
           playType=Routines.safeParseInt(request.getParameter("playType"));
           for(int currentType=0;currentType<typeOptions.length;currentType++)
              {
              if(playType==currentType)
                {
                webPageOutput.println(" <OPTION SELECTED VALUE=\"" + currentType + "\">" + typeOptions[currentType]);
                webPageOutput.println("</SELECT>");
                webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playType\" VALUE=\"" + playType + "\">");
                }
              }
            }
        Routines.tableDataEnd(false,false,false,webPageOutput);
        }
      //Load BallCarriers
      boolean multiRB=false;
      boolean multiFB=false;
      boolean multiWR=false;
      boolean multiTE=false;
      String[] backText=new String[3];
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Position1,Position2,Position3,Position4,Position5," +
                                     "Position6,Position7,Position8,Position9,Position10," +
                                     "Position11,Position12,Position13,Position14,Position15 " +
                                     "FROM formations " +
                                     "WHERE FormationNumber=" + formationNumber + " " +
                                     "ORDER BY Sequence ASC");
        queryResult.first();
        int[] positions=new int[15];
        int numOfRB=0;
        int numOfFB=0;
        int numOfWR=0;
        int numOfTE=0;
        int numOfDL=0;
        int numOfLB=0;
        int numOfDB=0;
        for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
           {
           positions[currentPosition]=queryResult.getInt(1 + currentPosition);
           if(currentPosition==14&&positions[currentPosition]==14)
             {
             shotGun=true;
             }
           if(positions[currentPosition]==15)
             {
             numOfRB++;
             if(numOfRB>1&&!multiRB)
               {
               multiRB=true;
               }
             }
           if(positions[currentPosition]==16)
             {
             numOfFB++;
             if(numOfFB>1&&!multiFB)
               {
               multiFB=true;
               }
             }
           if(positions[currentPosition]==13)
             {
             numOfWR++;
             if(numOfWR>1&&!multiWR)
               {
               multiWR=true;
               }
             }
           if(positions[currentPosition]==12)
             {
             numOfTE++;
             if(numOfTE>1&&!multiTE)
               {
               multiTE=true;
               }
             }
           }
        if(positions[1]!=0)
          {
          teLeft=true;
          }
        if(positions[7]!=0)
          {
          teRight=true;
          }
        if(positions[12]==15||positions[12]==16)
          {
          back1=true;
          }
        if(positions[13]==15||positions[13]==16)
          {
          back2=true;
          }
        if(positions[14]==15||positions[14]==16)
          {
          back3=true;
          }
        for(int currentDL=0;currentDL<5;currentDL++)
           {
           if(positions[currentDL]!=0)
             {
             numOfDL++;
             }
           if(positions[currentDL]<0)
             {
             numOfDL++;
             }
           }
        for(int currentLB=5;currentLB<10;currentLB++)
           {
           if(positions[currentLB]!=0)
             {
             numOfLB++;
             }
           }
        for(int currentDB=10;currentDB<15;currentDB++)
           {
           if(positions[currentDB]!=0)
             {
             numOfDB++;
             }
           if(positions[currentDB]<0)
             {
             numOfDB++;
             }
           }
        if(numOfDL>3)
          {
          allowStunt=true;
          }
        if(numOfLB>2&&numOfDB>3)
          {
          allowDouble=true;
          }
        if(numOfLB>1&&numOfDB==6)
          {
          allowTriple=true;
          }
        if(positions[5]!=0)
          {
          allowOLLB=true;
          }
        if(positions[6]!=0)
          {
          allowILLB=true;
          }
        if(positions[7]!=0)
          {
          allowMLB=true;
          }
        if(positions[8]!=0)
          {
          allowIRLB=true;
          }
        if(positions[9]!=0)
          {
          allowORLB=true;
          }
        if(positions[10]!=0)
          {
          allowLCB=true;
          }
        if(positions[11]!=0)
          {
          allowNB=true;
          }
        if(positions[12]!=0)
          {
          allowFS=true;
          }
        if(positions[13]!=0)
          {
          allowSS=true;
          }
        if(positions[14]!=0)
          {
          allowRCB=true;
          }
        if(positions[11]>30)
          {
          allowDB=true;
          }
        if((positions[12]==0&&(positions[13]==15||positions[13]==16)&&(positions[14]==15||positions[14]==16))||
           (positions[12]==0&&(positions[13]==15||positions[13]==16))||
           ((positions[12]==15||positions[12]==16)&&positions[13]==0))
          {
          allowPitch=true;
          }
        if(type==0&&playType==0)
          {
          int currentBallCarrier=0;
          int positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==15)
               {
               if(multiRB)
                 {
                 ballCarriers[currentBallCarrier]="RB" + positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="RB";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==16)
               {
               if(multiFB)
                 {
                 ballCarriers[currentBallCarrier]="FB" + positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="FB";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==14)
               {
               ballCarriers[currentBallCarrier]="QB";
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==13)
               {
               if(multiWR)
                 {
                 ballCarriers[currentBallCarrier]="WR"+positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="WR";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==12)
               {
               if(multiTE)
                 {
                 ballCarriers[currentBallCarrier]="TE"+positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="TE";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          }
        if(type==0&&playType==1)
          {
          int currentBallCarrier=0;
          int positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==13)
               {
               if(multiWR)
                 {
                 ballCarriers[currentBallCarrier]="WR"+positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="WR";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==12)
               {
               if(multiTE)
                 {
                 ballCarriers[currentBallCarrier]="TE"+positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="TE";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==15)
               {
               if(multiRB)
                 {
                 ballCarriers[currentBallCarrier]="RB" + positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="RB";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
          positionCount=1;
          for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
             {
             if(positions[currentPosition]==16)
               {
               if(multiFB)
                 {
                 ballCarriers[currentBallCarrier]="FB" + positionCount;
                 }
               else
                 {
                 ballCarriers[currentBallCarrier]="FB";
                 }
               ballCarrierNumbers[currentBallCarrier]=currentPosition+1;
               realPositionNumbers[currentBallCarrier]=positions[currentPosition];
               currentBallCarrier++;
               positionCount++;
               }
             }
             int rbCount=1;
             int fbCount=1;
             for(int currentBack=0;currentBack<backText.length;currentBack++)
                {
                if(positions[12+currentBack]==15)
                  {
                  if(numOfRB>1)
                    {
                    backText[currentBack]="RB"+rbCount+" Blocking";
                    rbCount++;
                    }
                  else
                    {
                    backText[currentBack]="RB Blocking";
                    }
                  }
                if(positions[12+currentBack]==16)
                  {
                  if(numOfFB>1)
                    {
                    backText[currentBack]="FB"+fbCount+" Blocking";
                    fbCount++;
                    }
                  else
                    {
                    backText[currentBack]="FB Blocking";
                    }
                  }
              }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find ballcarriers entries : " + error,false,context);
        }
      Routines.tableDataStart(true,true,false,false,false,17,0,"scoresrow",webPageOutput);
      if("Store Formation".equals(action)||"Update Formation".equals(action))
        {
        webPageOutput.println("<SELECT NAME=\"playNumber\">");
        for(int currentPlay=0;currentPlay<maxPlayNumber;currentPlay++)
           {
           if((playType==0&&playNumbers[currentPlay]==7&&shotGun)||
              (playType==0&&playNumbers[currentPlay]==8&&shotGun)||
              (playType==0&&playNumbers[currentPlay]==5&&!allowPitch)||
              (playType==0&&playNumbers[currentPlay]==6&&!allowPitch)||
              (playType==1&&playNumbers[currentPlay]==11&&!shotGun)||
              (playType==1&&playNumbers[currentPlay]==12&&shotGun)||
              (type==2&&playType==15&&(playNumbers[currentPlay]!=27&&
                                       playNumbers[currentPlay]!=28&&
                                       playNumbers[currentPlay]!=29&&
                                       playNumbers[currentPlay]!=30&&
                                       playNumbers[currentPlay]!=31))||
              (type==2&&playType==16&&(playNumbers[currentPlay]!=24&&
                                       playNumbers[currentPlay]!=25&&
                                       playNumbers[currentPlay]!=26))||
              (type==2&&playType==17&&(playNumbers[currentPlay]!=34&&
                                       playNumbers[currentPlay]!=35&&
                                       playNumbers[currentPlay]!=36&&
                                       playNumbers[currentPlay]!=37&&
                                       playNumbers[currentPlay]!=38&&
			                           playNumbers[currentPlay]!=41))||
              (type==2&&playType==18&&(playNumbers[currentPlay]!=32&&
                                       playNumbers[currentPlay]!=33))||
              (type==2&&formationNumber==33&&(playNumbers[currentPlay]!=27&&playNumbers[currentPlay]!=28))||
              (type==2&&formationNumber==35&&(playNumbers[currentPlay]!=29&&playNumbers[currentPlay]!=30&&playNumbers[currentPlay]!=31))||
              (type==2&&formationNumber==37&&(playNumbers[currentPlay]!=34&&playNumbers[currentPlay]!=35))||
              (type==2&&formationNumber==38&&(playNumbers[currentPlay]!=36&&playNumbers[currentPlay]!=37&&playNumbers[currentPlay]!=38&&playNumbers[currentPlay]!=41))||
              (type==2&&formationNumber==39&&(playNumbers[currentPlay]!=36&&playNumbers[currentPlay]!=37&&playNumbers[currentPlay]!=38)))
             {
             }
           else
             {
             if(playTypes[currentPlay]==playType||type==2)
               {
               if(playNumber==playNumbers[currentPlay])
                 {
                 selectText=" SELECTED";
                 }
               else
                 {
                 selectText="";
                 }
             webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + playNumbers[currentPlay] + "\">" + playNames[currentPlay]);
               }
             }
           }
        webPageOutput.println("</SELECT>");
        }
      else
        {
        if("Store Play".equals(action)||"Update Play".equals(action)||
           "Store Player".equals(action)||"Update Player".equals(action))
          {
          playNumber=Routines.safeParseInt(request.getParameter("playNumber"));
          }
        webPageOutput.println("<SELECT DISABLED NAME=\"playNumber\">");
        for(int currentPlay=0;currentPlay<maxPlayNumber;currentPlay++)
           {
           if(playNumber==playNumbers[currentPlay])
             {
             webPageOutput.println(" <OPTION SELECTED VALUE=\"" + playNumbers[currentPlay] + "\">" + playNames[currentPlay]);
             webPageOutput.println("</SELECT>");
             webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playNumber\" VALUE=\"" + playNumber + "\">");
             }
           }
        }
      Routines.tableDataEnd(false,false,false,webPageOutput);
      if(type==0)
        {
        Routines.tableDataStart(true,true,false,false,false,17,0,"scoresrow",webPageOutput);
        if("Store Play".equals(action)||"Update Play".equals(action))
          {
          if(tempFormationNumber!=formationNumber)
            {
            positionNumber=0;
            }
          webPageOutput.println("<SELECT NAME=\"positionNumber\">");
          for(int currentPosition=0;currentPosition<ballCarrierNumbers.length;currentPosition++)
             {
             if((playType==0&&realPositionNumbers[currentPosition]==14&&playNumber==9)||
                (playType==0&&realPositionNumbers[currentPosition]==15&&playNumber==9)||
                (playType==0&&realPositionNumbers[currentPosition]==16&&playNumber==9)||
                (playType==0&&(realPositionNumbers[currentPosition]==12||realPositionNumbers[currentPosition]==13)&&playNumber!=9)||
                (playType==0&&(realPositionNumbers[currentPosition]!=15&&realPositionNumbers[currentPosition]!=16)&&(playNumber==5||playNumber==6))||
                (playType==0&&realPositionNumbers[currentPosition]!=14&&(playNumber==7||playNumber==8))||
                (playType==1&&(realPositionNumbers[currentPosition]!=15&&realPositionNumbers[currentPosition]!=16)&&playNumber==11)||
                (playType==1&&(realPositionNumbers[currentPosition]!=15&&realPositionNumbers[currentPosition]!=16)&&playNumber==10))
                {
                }
              else
                {
                if(ballCarrierNumbers[currentPosition]!=0)
                  {
                  if(positionNumber==ballCarrierNumbers[currentPosition])
                    {
                    selectText=" SELECTED";
                    realPositionNumber=realPositionNumbers[currentPosition];
                    }
                  else
                    {
                    selectText="";
                    }
                  webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + ballCarrierNumbers[currentPosition] + "\">" + ballCarriers[currentPosition]);
                  }
                }
             }
          webPageOutput.println("</SELECT>");
          }
        else
          {
          if("Store Player".equals(action)||"Update Player".equals(action))
            {
            positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
            }
          webPageOutput.println("<SELECT DISABLED NAME=\"positionNumber\">");
          for(int currentPosition=0;currentPosition<ballCarriers.length;currentPosition++)
             {
             if(positionNumber==ballCarrierNumbers[currentPosition])
               {
               String positionText="";
               if(positionNumber!=0)
                 {
                 positionText=ballCarriers[currentPosition];
                 }
               webPageOutput.println(" <OPTION SELECTED VALUE=\"" + positionNumber + "\">" + positionText);
               webPageOutput.println("</SELECT>");
               webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"positionNumber\" VALUE=\"" + positionNumber + "\">");
               realPositionNumber=realPositionNumbers[currentPosition];
               }
             }
          }
        Routines.tableDataEnd(false,false,false,webPageOutput);
        }
      if(type!=2)
        {
        Routines.tableDataStart(true,true,false,false,false,17,0,"scoresrow",webPageOutput);
        if((type==1&&playType==0)||type==1&&playType==1&&playNumber==23)
          {
          webPageOutput.println("None");
          webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"primaryStrategyNumber\" VALUE=\"68\">");
          }
        else
          {
          if("Store Player".equals(action)||"Update Player".equals(action)||
            (("Store Play".equals(action)||"Update Play".equals(action))&&type==1))
            {
            webPageOutput.println("<SELECT NAME=\"primaryStrategyNumber\">");
            for(int currentStrategy=0;currentStrategy<maxPrimaryStrategyNumber;currentStrategy++)
               {
               if((type==0&&playType==0&&realPositionNumber!=14&&primaryStrategyNumbers[currentStrategy]==1)||
                  (type==0&&playType==0&&realPositionNumber!=14&&primaryStrategyNumbers[currentStrategy]==2)||
                  (type==0&&playType==0&&realPositionNumber==14&&primaryStrategyNumbers[currentStrategy]==1&&(playNumber!=1&&playNumber!=2))||
                  (type==0&&playType==0&&realPositionNumber==14&&primaryStrategyNumbers[currentStrategy]==2&&(playNumber!=1&&playNumber!=2))||
                  (type==0&&playType==0&&primaryStrategyNumbers[currentStrategy]==3)||
                  (type==0&&playType==0&&primaryStrategyNumbers[currentStrategy]==5)||
                  (type==0&&playType==0&&primaryStrategyNumbers[currentStrategy]==4&&(playNumber!=1&&playNumber!=2))||
                  (type==0&&playType==1&&(primaryStrategyNumbers[currentStrategy]==1||primaryStrategyNumbers[currentStrategy]==2))||
                  (type==0&&playType==1&&primaryStrategyNumbers[currentStrategy]==3&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
                  (type==0&&playType==1&&primaryStrategyNumbers[currentStrategy]==4)||
                  (type==0&&playType==1&&primaryStrategyNumbers[currentStrategy]==5&&shotGun)||
                  (type==0&&playType==1&&primaryStrategyNumbers[currentStrategy]==5&&(playNumber!=14&&playNumber!=15&&playNumber!=39))||
                  (type==1&&playType==1&&primaryStrategyNumbers[currentStrategy]==15&&!allowDouble)||
                  (type==1&&playType==1&&primaryStrategyNumbers[currentStrategy]==16&&!allowTriple)||
                  (type==1&&playType==1&&primaryStrategyNumbers[currentStrategy]==17&&!allowTriple))
                 {
                 }
               else
                 {
                 if(primaryStrategyNumber==primaryStrategyNumbers[currentStrategy])
                   {
                   selectText=" SELECTED";
                   }
                 else
                   {
                   selectText="";
                   }
                 webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + primaryStrategyNumbers[currentStrategy] + "\">" + primaryStrategyNames[currentStrategy]);
                 }
               }
            webPageOutput.println("</SELECT>");
            }
          else
            {
            webPageOutput.println("<SELECT DISABLED NAME=\"primaryStrategyNumber\">");
            for(int currentStrategy=0;currentStrategy<maxPrimaryStrategyNumber;currentStrategy++)
               {
               if(primaryStrategyNumber==primaryStrategyNumbers[currentStrategy])
                 {
                 webPageOutput.println(" <OPTION SELECTED VALUE=\"" + primaryStrategyNumbers[currentStrategy] + "\">" + primaryStrategyNames[currentStrategy]);
                 webPageOutput.println("</SELECT>");
                 webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"primaryStrategyNumber\" VALUE=\"" + primaryStrategyNumber + "\">");
                 }
               }
            }
          }
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      if(type!=2)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableDataStart(true,true,true,true,false,0,8,"scoresrow",webPageOutput);
        webPageOutput.println(Routines.indent(1));
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableDataStart(true,true,true,true,false,0,3,"scoresrow",webPageOutput);
        webPageOutput.println("Secondary Strategies");
        Routines.tableDataEnd(true,false,true,webPageOutput);
        int fieldCount=0;
        boolean lineFilled=true;
        disabledText="";
        if("Store Player".equals(action)||"Update Player".equals(action)||
          (("Store Play".equals(action)||"Update Play".equals(action))&&type==1))
          {
          }
        else
          {
          disabledText=" DISABLED";
          }
        for(int currentStrategy=0;currentStrategy<maxSecondaryStrategyNumber;currentStrategy++)
           {
           if((type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==7)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==8)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==9)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==10)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==11)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==12)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==13)||
              (type==0&&playType==0&&secondaryStrategyNumbers[currentStrategy]==14)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==8&&playNumber!=15)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==9&&playNumber!=15)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==10&&!teLeft)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==11&&!teRight)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==10&&positionNumber==2)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==11&&positionNumber==8)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==12&&!back1)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==13&&!back2)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==14&&!back3)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==10&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==11&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==12&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==13&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==14&&(playNumber!=13&&playNumber!=14&&playNumber!=15&&playNumber!=39))||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==12&&positionNumber==13)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==13&&positionNumber==14)||
              (type==0&&playType==1&&secondaryStrategyNumbers[currentStrategy]==14&&positionNumber==15)||
              (type==1&&secondaryStrategyNumbers[currentStrategy]==66&&!allowStunt)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==19)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==20)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==21)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==22)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==23)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==24)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==25)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==26)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==27)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==28)||
              (type==1&&playType==0&&secondaryStrategyNumbers[currentStrategy]==67)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==19&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==20&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==21&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==22&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==23&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==24&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==25&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==26&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==27&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==28&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==67&&playNumber!=23)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==19&&!allowOLLB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==20&&!allowILLB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==21&&!allowMLB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==22&&!allowIRLB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==23&&!allowORLB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==24&&!allowLCB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==25&&!allowNB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==26&&!allowFS)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==27&&!allowSS)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==28&&!allowRCB)||
              (type==1&&playType==1&&secondaryStrategyNumbers[currentStrategy]==67&&!allowDB))
             {
             }
           else
             {
             if(fieldCount==0)
               {
               Routines.tableDataStart(true,false,false,true,false,33,0,"scoresrow",webPageOutput);
               }
             else
               {
               Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
               }
             webPageOutput.print("<INPUT" + disabledText + " TYPE=\"CHECKBOX\" NAME=\"SecStrat" + secondaryStrategyNumbers[currentStrategy] + "\" VALUE=\"1\"");
             if(secondaryStrategyNumber!=null)
               {
               for(int selectedStrategy=0;selectedStrategy<secondaryStrategyNumber.length;selectedStrategy++)
                  {
                  if(secondaryStrategyNumbers[currentStrategy]==secondaryStrategyNumber[selectedStrategy])
                    {
                    webPageOutput.print(" CHECKED");
                    }
                  }
               }
             webPageOutput.println(">");
             if(secondaryStrategyNumbers[currentStrategy]==10)
               {
               if(teRight)
                 {
                 secondaryStrategyNames[currentStrategy]="TE1 Blocking";
                 }
               else
                 {
                 secondaryStrategyNames[currentStrategy]="TE Blocking";
                 }
               }
             if(secondaryStrategyNumbers[currentStrategy]==11)
               {
               if(teLeft)
                 {
                 secondaryStrategyNames[currentStrategy]="TE2 Blocking";
                 }
               else
                 {
                 secondaryStrategyNames[currentStrategy]="TE Blocking";
                 }
              }
             if(secondaryStrategyNumbers[currentStrategy]==12)
               {
               secondaryStrategyNames[currentStrategy]=backText[0];
               }
             if(secondaryStrategyNumbers[currentStrategy]==13)
               {
               secondaryStrategyNames[currentStrategy]=backText[1];
               }
             if(secondaryStrategyNumbers[currentStrategy]==14)
               {
               secondaryStrategyNames[currentStrategy]=backText[2];
               }
             webPageOutput.println(secondaryStrategyNames[currentStrategy]);
             if(fieldCount==2)
               {
               Routines.tableDataEnd(true,false,true,webPageOutput);
               fieldCount=0;
               lineFilled=true;
               }
             else
               {
               Routines.tableDataEnd(false,false,false,webPageOutput);
               fieldCount++;
               }
             }
           }
        if(lineFilled&&fieldCount==1)
           {
           Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
           Routines.tableDataEnd(false,false,false,webPageOutput);
           Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
           Routines.tableDataEnd(true,false,true,webPageOutput);
           }
         if(lineFilled&&fieldCount==2)
           {
           Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
           Routines.tableDataEnd(true,false,true,webPageOutput);
           }
          Routines.tableDataEnd(false,false,true,webPageOutput);
          Routines.tableEnd(webPageOutput);
          }
      }

   private synchronized boolean updateEntry(int type,
                                            String action,
                                            int maxSecondaryStrategyNumber,
                                            int[] secondaryStrategyNumbers,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int situationNumber=Routines.safeParseInt(request.getParameter("situationNumber"));
      int playBookNumber=Routines.safeParseInt(request.getParameter("playBookNumber"));
      int setNumber=Routines.safeParseInt(request.getParameter("setNumber"));
      int formationNumber=Routines.safeParseInt(request.getParameter("formationNumber"));
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int playNumber=Routines.safeParseInt(request.getParameter("playNumber"));
      int primaryStrategyNumber=Routines.safeParseInt(request.getParameter("primaryStrategyNumber"));
      int sequence=0;
      int secondaryStrategySet=0;
      int[] secondaryStrategyNumber=new int[10];
      int maxChosenSecondaryStrategy=0;
      if(type==2)
        {
        primaryStrategyNumber=69;
        }
      for(int currentStrategy=0;currentStrategy<maxSecondaryStrategyNumber;currentStrategy++)
         {
         secondaryStrategySet=Routines.safeParseInt(request.getParameter("SecStrat" + String.valueOf(secondaryStrategyNumbers[currentStrategy])));
         if(secondaryStrategySet==1)
           {
           secondaryStrategyNumber[maxChosenSecondaryStrategy]=secondaryStrategyNumbers[currentStrategy];
           maxChosenSecondaryStrategy++;
           }
         }
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM defaultplaybook " +
                                     "WHERE SituationNumber=" + situationNumber + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(playBookNumber==0)
          {
          //Get latest playBookNumber.
          playBookNumber=1;
          queryResult=sql.executeQuery("SELECT PlayBookNumber " +
                                       "FROM defaultplaybook " +
                                       "ORDER BY PlayBookNumber DESC");
          if(queryResult.first())
            {
            playBookNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve defaultplaybook : " + error,false,context);
        }
      if("Move Entry Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM defaultplaybook " +
                                         "WHERE SituationNumber=" + situationNumber + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               playBookNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence FROM defaultplaybook " +
                                                "WHERE PlayBookNumber=" + playBookNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message","Entry is already at the top of the PlayBook list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultplaybook " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SituationNumber=" + situationNumber + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultplaybook " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultplaybook : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No entry selected");
          }
        success=true;
        }
      if("Move Entry Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM defaultplaybook " +
                                         "WHERE SituationNumber=" + situationNumber + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               playBookNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence FROM defaultplaybook " +
                                                "WHERE PlayBookNumber=" + playBookNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message","Entry is already at the bottom of the PlayBook list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultplaybook " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SituationNumber=" + situationNumber + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultplaybook " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultplaybook : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No entries selected");
          }
        success=true;
        }
      if("Store Strats".equals(action)||"Store Play".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO defaultplaybook (" +
                                    "PlayBookNumber,SituationNumber,Sequence," +
                                    "SetNumber,FormationNumber,PositionNumber,PlayNumber," +
                                    "PrimaryStrategyNumber," +
                                    "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                    "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                    "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                    "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                    "SecondaryStrategyNumber9,SecondaryStrategyNumber10," +
                                    "DateTimeStamp) " +
                                    "VALUES (" +
                                    playBookNumber + "," +
                                    situationNumber + "," +
                                    (sequence+1) + "," +
                                    setNumber + "," +
                                    formationNumber + "," +
                                    positionNumber + "," +
                                    playNumber + "," +
                                    primaryStrategyNumber + "," +
                                    secondaryStrategyNumber[0] + "," +
                                    secondaryStrategyNumber[1] + "," +
                                    secondaryStrategyNumber[2] + "," +
                                    secondaryStrategyNumber[3] + "," +
                                    secondaryStrategyNumber[4] + "," +
                                    secondaryStrategyNumber[5] + "," +
                                    secondaryStrategyNumber[6] + "," +
                                    secondaryStrategyNumber[7] + "," +
                                    secondaryStrategyNumber[8] + "," +
                                    secondaryStrategyNumber[9] + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"Store defaultplaybook not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create defaultplaybook : " + error,false,context);
          }
        session.setAttribute("message","Entry stored successfully");
        success=true;
        }
      if("Update Strats".equals(action)||"Update Play".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE defaultplaybook " +
                                    "SET SetNumber=" + setNumber + "," +
                                    "FormationNumber=" + formationNumber + "," +
                                    "PositionNumber=" + positionNumber + "," +
                                    "PlayNumber=" + playNumber + "," +
                                    "PrimaryStrategyNumber=" + primaryStrategyNumber + "," +
                                    "SecondaryStrategyNumber1=" + secondaryStrategyNumber[0] + "," +
                                    "SecondaryStrategyNumber2=" + secondaryStrategyNumber[1] + "," +
                                    "SecondaryStrategyNumber3=" + secondaryStrategyNumber[2] + "," +
                                    "SecondaryStrategyNumber4=" + secondaryStrategyNumber[3] + "," +
                                    "SecondaryStrategyNumber5=" + secondaryStrategyNumber[4] + "," +
                                    "SecondaryStrategyNumber6=" + secondaryStrategyNumber[5] + "," +
                                    "SecondaryStrategyNumber7=" + secondaryStrategyNumber[6] + "," +
                                    "SecondaryStrategyNumber8=" + secondaryStrategyNumber[7] + "," +
                                    "SecondaryStrategyNumber9=" + secondaryStrategyNumber[8] + "," +
                                    "SecondaryStrategyNumber10=" + secondaryStrategyNumber[9] + "," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE PlayBookNumber=" + playBookNumber);
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"defaultplaybook not updated, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update defaultplaybook : " + error,false,context);
          }
        session.setAttribute("message","Entry changed successfully");
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
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM defaultplaybook");
          while(queryResult1.next())
               {
               playBookNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM defaultplaybook " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook not deleted (" + playBookNumber + ")",false,context);
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM defaultplaybook " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          playBookNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                playBookNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE defaultplaybook " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false
                                           ) + "' " +
                                           "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultplaybook entry not reset (" + playBookNumber + ")",false,context);
                   }
                }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete defaultplaybook : " + error,false,context);
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