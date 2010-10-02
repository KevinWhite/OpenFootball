import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class LeagueRunner extends HttpServlet implements Runnable
   {
   private ServletContext context;
   private int leagueNumber=0;
   private int season=0;
   private int week=0;
   private int status=0;
   private int leagueLevel=0;
   private int numOfWeeks=0;
   private int numOfGames=0;
   private int numOfWeeksBeforePostSeason=0;
   private boolean active=true;
   private static String servletName="LeagueRunner";
   private Properties properties;
   private GameRunner[] gameRunners=null;
   private Thread[] threadArray=null;
   private static String[] list;
   private static int totalNumberOfLines=0;
   private ConnectionPool pool;

   LeagueRunner(ServletContext context,int leagueNumber)
      {
      this.context=context;
      this.leagueNumber=leagueNumber;
	  File path = new File("../Source");
	  list = path.list(new DirectoryFilter("java"));
	  for(int currentFile=0;currentFile<list.length;currentFile++)
		 {
	     try
	       {
		   FileReader file=new FileReader("../Source\\"+list[currentFile]);
		   BufferedReader fileBuffer=new BufferedReader(file);
		   boolean endOfFile=false;
		   int numOfLines=0;
		   while(!endOfFile)
		    {
			String line=fileBuffer.readLine();
			if(line==null)
			  {
			  endOfFile=true;
			  System.out.println(list[currentFile] + "=" + numOfLines);
			  }
			else
			  {
			  numOfLines++;	
			  totalNumberOfLines++;
			  }
			}
	       }	  
		 catch(IOException error) 
		   {
		   Routines.writeToLog(servletName,"Error getting class : " + error,false,context);
		   }  			 	
		 }	      
	  System.out.println("TotalLines="+totalNumberOfLines);	
	  }

   public void run()
      {
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
		Connection database=null;
		try
		  {
		  database=pool.getConnection(servletName);
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
		  }
        //TEST CODE!!!
		try
		  {
		  Statement sql=database.createStatement();
		  sql.executeUpdate("DELETE FROM playbyplay");
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Error deleting playbyplay("+leagueNumber+")"+" : " + error,false,context);
		  }
		///END OF TEST CODE!!!        
		try
        {
        Statement sql=database.createStatement();
        int updated=sql.executeUpdate("UPDATE leagues " +
                                      "SET LockDown=1 " +
                                      "WHERE LeagueNumber="+leagueNumber);
        if(updated!=1)
          {
          Routines.writeToLog(servletName,"Unable to lock League(" + leagueNumber+")",false,context);
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Error locking league("+leagueNumber+")"+" : " + error,false,context);
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Season,Week,Status,LeagueLevel,SUM(PreSeasonWeeks+RegularSeasonWeeks+PostSeasonWeeks),SUM(PreSeasonWeeks+RegularSeasonWeeks) " +
                                     "FROM leagues " +
                                     "WHERE LeagueNumber="+leagueNumber+" "+
                                     "GROUP BY LeagueNumber");
        queryResult.first();
        season=queryResult.getInt(1);
        week=queryResult.getInt(2);
        status=queryResult.getInt(3);
        leagueLevel=queryResult.getInt(4);
        numOfWeeks=queryResult.getInt(5);
        numOfWeeksBeforePostSeason=queryResult.getInt(6);
        if(week==0&&status==1)
          {
          Routines.writeToLog(servletName,"Draft Required",false,context);
          int startingRound=0;
          int startingTeam=0;
          Routines.writeToLog(servletName,"Processing Staff Signings",false,context);
          boolean success=Routines.processStaffSignings(leagueNumber,database,context,properties);
          Routines.writeToLog(servletName,"Ended Staff Signings",false,context);
          if(!success)
            {
            Routines.writeToLog(servletName,"Failed to process staff signing",false,context);
            }
//          database.close();
//          database=null;
//          try
//            {
//            database=DriverManager.getConnection(properties.getProperty("connection.url"),properties);
//            }
//          catch(SQLException error)
//            {
//            Routines.writeToLog(servletName,"Error connecting to database(2) : " + error,false,context);
//            }
          sql=database.createStatement();
          try
            {
            queryResult=sql.executeQuery("SELECT Round,Sequence " +
                                         "FROM draftboard " +
                                         "WHERE LeagueNumber=" + leagueNumber);
            queryResult.first();
            startingRound=queryResult.getInt(1);
            startingTeam=queryResult.getInt(2);
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error counting positions : " + error,false,context);
            }
          int numOfRounds=0;
          try
            {
            queryResult=sql.executeQuery("SELECT COUNT(PositionNumber) " +
                                         "FROM positions " +
                                         "WHERE RealPosition=1 " +
                                         "AND Type!=3");
            queryResult.first();
            numOfRounds=queryResult.getInt(1)*4;
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error counting positions 2 : " + error,false,context);
            }
          boolean flipFlop=true;
          int numOfTeams=0;
          try
            {
            queryResult=sql.executeQuery("SELECT COUNT(TeamNumber) " +
                                         "FROM draftboardteam " +
                                         "WHERE LeagueNumber=" + leagueNumber);
            queryResult.first();
            numOfTeams=queryResult.getInt(1);
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error counting teams : " + error,false,context);
            }
          int[] draftOrder=new int[numOfTeams];
          try
            {
            queryResult=sql.executeQuery("SELECT TeamNumber " +
                                         "FROM draftboardteam " +
                                         "WHERE LeagueNumber=" + leagueNumber + " " +
                                         "ORDER BY Sequence ASC");
            int currentTeam=0;
            while(queryResult.next())
                 {
                 draftOrder[currentTeam]=queryResult.getInt(1);
                 currentTeam++;
                 }
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error counting teams 2 : " + error,false,context);
            }
          int dbRound=0;
          boolean firstTime=true;
          for(int currentRound=startingRound;currentRound<numOfRounds;currentRound++)
             {
//             database.close();
//             database=null;
//             try
//               {
//               database=DriverManager.getConnection(properties.getProperty("connection.url"),properties);
//               }
//             catch(SQLException error)
//               {
//               Routines.writeToLog(servletName,"Error connecting to database(3) : " + error,false,context);
//               }
             sql=database.createStatement();
             System.out.println("Round="+currentRound);
             dbRound++;
             if(dbRound>numOfRounds/4)
               {
               dbRound=1;
               }
             for(int currentTeam=startingTeam;currentTeam<numOfTeams;currentTeam++)
                {
                int updated=sql.executeUpdate("UPDATE draftboard " +
                                              "SET Round=" + currentRound + "," +
                                              "Sequence=" + currentTeam + "," +
                                              "Selection=0 " +
                                              "WHERE LeagueNumber=" + leagueNumber);
                 if(updated!=1)
                   {
                   Routines.writeToLog(servletName,"draftboard not updated (LeagueNumber=" +
                                       leagueNumber + ")",false,context);
                   }
                int selection=0;
                if((currentRound+1)<=(numOfRounds/2))
                  {
                  try
                    {
                    queryResult=sql.executeQuery("SELECT Selection " +
                                                 "FROM draftboard " +
                                                 "WHERE LeagueNumber=" + leagueNumber);
                    queryResult.first();
                    selection=queryResult.getInt(1);
                    }
                  catch(SQLException error)
                    {
                    Routines.writeToLog(servletName,"Error looking for selection : " + error,false,context);
                    }
                  }
                if(selection==0)
                  {
                  int currentPick=0;
                  if(flipFlop)
                    {
                    currentPick=currentTeam;
                    }
                  else
                    {
                    currentPick=numOfTeams-(currentTeam+1);
                    }
                  int negotiator=0;
                  try
                    {
                    queryResult=sql.executeQuery("SELECT Skill1 " +
                                                 "FROM players " +
                                                 "WHERE TeamNumber=" + draftOrder[currentPick] + " " +
                                                 "AND PositionNumber=51");
                    if(queryResult.first())
                      {
                      negotiator=(queryResult.getInt(1)+5)/5;
                      }
                    }
                  catch(SQLException error)
                    {
                    Routines.writeToLog(servletName,"Error retrieving negotiator("+draftOrder[currentPick]+") : " + error,false,context);
                    }
                  try
                    {
		            queryResult=sql.executeQuery("SELECT players.PlayerNumber,COUNT(PositionSkillNumber)," +
                                                 "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                                 "Skill6,Skill7,Skill8,Skill9,Skill10," +
                                                 "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                                 "Skill16,Skill17,Skill18,Skill19,Skill20 " +
                                                 "FROM draftpriorities,players,draftratings,positionskills " +
                                                 "WHERE draftpriorities.TeamNumber=" + draftOrder[currentPick] + " " +
                                                 "AND draftpriorities.Sequence=" + dbRound + " " +
                                                 "AND draftpriorities.PositionNumber=players.PositionNumber " +
                                                 "AND players.PlayerNumber=draftratings.PlayerNumber " +
                                                 "AND draftratings.TeamNumber=" + draftOrder[currentPick] + " " +
                                                 "AND players.WorldNumber=" + leagueNumber + " " +
                                                 "AND players.PositionNumber = positionskills.PositionNumber " +
                                                 "AND players.Experience=0 " +
                                                 "AND players.TeamNumber=0 " +
                                                 "GROUP BY players.PlayerNumber " +
                                                 "ORDER BY PositionRating ASC");
                    while(queryResult.next()&&selection==0)
                         {
                         selection=queryResult.getInt(1);
                         int numOfSkills=queryResult.getInt(2);
                         int skills[]=new int[25];
                         skills[5]=queryResult.getInt(3);
                         skills[6]=queryResult.getInt(4);
                         skills[7]=queryResult.getInt(5);
                         skills[8]=queryResult.getInt(6);
                         skills[9]=queryResult.getInt(7);
                         skills[10]=queryResult.getInt(8);
                         skills[11]=queryResult.getInt(9);
                         skills[12]=queryResult.getInt(10);
                         skills[13]=queryResult.getInt(11);
                         skills[14]=queryResult.getInt(12);
                         skills[15]=queryResult.getInt(13);
                         skills[16]=queryResult.getInt(14);
                         skills[17]=queryResult.getInt(15);
                         skills[18]=queryResult.getInt(16);
                         skills[19]=queryResult.getInt(17);
                         skills[20]=queryResult.getInt(18);
                         skills[21]=queryResult.getInt(19);
                         skills[22]=queryResult.getInt(20);
                         skills[23]=queryResult.getInt(21);
                         skills[24]=queryResult.getInt(22);
                         if((currentRound+1)>(numOfRounds/2))
                           {
                           int myRating=Routines.howGoodAmI(selection,skills,numOfSkills);
                           if(myRating>(10+negotiator))
                             {
							 selection=0;
                             }
                           }
                         }
                    if(selection==0)
                      {
                      Routines.writeToLog(servletName,"No player selected[Round="+dbRound+",Pick="+currentPick,false,context);
                      }
                    }
                  catch(SQLException error)
                    {
                    Routines.writeToLog(servletName,"Error getting selection : " + error,false,context);
                    Routines.writeToLog(servletName,"CurrentPick=" + currentPick,false,context);
                    Routines.writeToLog(servletName,"TeamNumber=" + draftOrder[currentPick],false,context);
                    Routines.writeToLog(servletName,"dbRound=" + dbRound,false,context);
                    Routines.writeToLog(servletName,"LeagueNumber=" + leagueNumber,false,context);
                    Routines.writeToLog(servletName,"",false,context);
                    }
                  if(firstTime)
                    {
                    //Restart procedure
                    queryResult=sql.executeQuery("SELECT PlayerNumber " +
                                                 "FROM players " +
                                                 "WHERE WorldNumber=" + leagueNumber + " " +
                                                 "AND DraftedSeason=" + season + " " +
                                                 "AND DraftedRound=" + (currentRound+1) + " " +
                                                 "AND DraftedPick=" + (currentPick+1));
                    if(queryResult.first())
                      {
                      }
                    else
                      {
                      //Assign player to team
                      updated=sql.executeUpdate("UPDATE players " +
                                                "SET TeamNumber=" + draftOrder[currentPick] + "," +
                                                "DraftedSeason=" + season + "," +
                                                "DraftedRound=" + (currentRound+1) + "," +
                                                "DraftedPick=" + (currentTeam+1) + "," +
                                                "DraftedTeam=" + draftOrder[currentPick] + " " +
                                                "WHERE PlayerNumber=" + selection);
                      if(updated!=1)
                        {
                        Routines.writeToLog(servletName,"Player not updated (PlayerNumber="+selection+")",false,context);
                        }
                      }
                    }
                  else
                    {
                      //Assign player to team
                      updated=sql.executeUpdate("UPDATE players " +
                                                "SET TeamNumber=" + draftOrder[currentPick] + "," +
                                                "DraftedSeason=" + season + "," +
                                                "DraftedRound=" + (currentRound+1) + "," +
                                                "DraftedPick=" + (currentTeam+1) + "," +
                                                "DraftedTeam=" + draftOrder[currentPick] + " " +
                                                "WHERE PlayerNumber=" + selection);
                      if(updated!=1)
                        {
                        Routines.writeToLog(servletName,"Player not updated (PlayerNumber="+selection+")",false,context);
                        }
                    }
                  }
                }
             startingTeam=0;
             if(flipFlop)
               {
               flipFlop=false;
               }
             else
               {
               flipFlop=true;
               }
             firstTime=false;
             }
          //Now assign shirt numbers
          try
            {
            queryResult=sql.executeQuery("SELECT PlayerNumber,TeamNumber,players.PositionNumber,ShirtMin1,ShirtMax1,ShirtMin2,ShirtMax2,PreferredNumber " +
                                         "FROM players,positions " +
                                         "WHERE WorldNumber=" + leagueNumber + " " +
                                         "AND players.PositionNumber=positions.PositionNumber " +
                                         "AND TeamNumber!=0 " +
                                         "ORDER BY TeamNumber ASC, players.PositionNumber ASC");
            int positionNumber=0;
            int teamNumber=0;
            int[] numberArray=null;
            boolean allNumbers[]=null;
            while(queryResult.next())
              {
              int playerNumber=queryResult.getInt(1);
              int preferredNumber=queryResult.getInt(8);
              if(teamNumber!=queryResult.getInt(2)||positionNumber!=queryResult.getInt(3))
                {
                if(teamNumber!=queryResult.getInt(2))
                  {
                  allNumbers=new boolean[100];
                  }
                teamNumber=queryResult.getInt(2);
                positionNumber=queryResult.getInt(3);
                int minShirt1=queryResult.getInt(4);
                int maxShirt1=queryResult.getInt(5);
                int minShirt2=queryResult.getInt(6);
                int maxShirt2=queryResult.getInt(7);
                int availableNumbers=0;
                for(int currentShirt=minShirt1;currentShirt<=maxShirt1;currentShirt++)
                   {
                   if(!allNumbers[currentShirt])
                     {
                     availableNumbers++;
                     }
                   }
                for(int currentShirt=minShirt2;currentShirt<=maxShirt2;currentShirt++)
                   {
                   if(!allNumbers[currentShirt])
                     {
                     availableNumbers++;
                     }
                   }
                numberArray=new int[availableNumbers];
                int currentElement=0;
                for(int currentShirt=minShirt1;currentShirt<=maxShirt1;currentShirt++)
                   {
                   if(!allNumbers[currentShirt])
                     {
                     numberArray[currentElement]=currentShirt;
                     currentElement++;
                     }
                   }
                for(int currentShirt=minShirt2;currentShirt<=maxShirt2;currentShirt++)
                   {
                   if(!allNumbers[currentShirt])
                     {
                     numberArray[currentElement]=currentShirt;
                     currentElement++;
                     }
                   }
                }
              int selectedNumber=0;
              if(numberArray.length>0)
                {
                for(int currentNumber=0;currentNumber<numberArray.length;currentNumber++)
                   {
                   if(numberArray[currentNumber]==preferredNumber)
                     {
                     selectedNumber=preferredNumber;
                     allNumbers[selectedNumber]=true;
                     }
                   }
                if(selectedNumber==0)
                  {
                  Random random=new Random(playerNumber);
                  int shirtNumber=((int)(random.nextDouble()*numberArray.length));
                  selectedNumber=numberArray[shirtNumber];
                  allNumbers[selectedNumber]=true;
                  }
                }
              try
                {
                int updated=sql.executeUpdate("UPDATE players " +
                                              "SET Number=" +
                                              selectedNumber + " " +
                                              "WHERE PlayerNumber=" + playerNumber);
                if(updated!=1)
                  {
                  Routines.writeToLog(servletName,"player not updated for shirt number("+playerNumber+")",false,context);
                  }
                }
              catch(SQLException error)
                {
                Routines.writeToLog(servletName,"Error updating player shirt number("+playerNumber+") : " + error,false,context);
                }
              if(numberArray.length>1)
                {
                int newArray[]=new int[(numberArray.length)-1];
                boolean found=false;
                int newArrayItem=0;
                for(int currentNumber=0;currentNumber<numberArray.length;currentNumber++)
                   {
                   if(numberArray[currentNumber]==selectedNumber)
                     {
                     found=true;
                     }
                   else
                     {
                     newArray[newArrayItem]=numberArray[currentNumber];
                     newArrayItem++;
                     }
                   }
                if(!found)
                  {
                  Routines.writeToLog(servletName,"Shirt("+selectedNumber+") not found in array",false,context);
                  }
                numberArray=newArray;
                }
              }
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error getting players for shirt numbering : " + error,false,context);
            }
          //Create depth charts
          try
            {
            queryResult=sql.executeQuery("SELECT TeamNumber " +
                                         "FROM leagueteams,divisions,conferences " +
                                         "WHERE leagueteams.DivisionNumber=divisions.DivisionNumber " +
                                         "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
                                         "AND conferences.LeagueNumber=" + leagueNumber);
            while(queryResult.next())
               {
               int updated=sql.executeUpdate("DELETE FROM depthcharts " +
                                             "WHERE TeamNumber = " + queryResult.getInt(1));
               }
            }
          catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error getting teams : " + error,false,context);
            }
          System.out.println("Finished cleaning");
          int sets[]=null;
          String setNames[]=null;
          try
            {
            queryResult=sql.executeQuery("SELECT TeamNumber,players.PositionNumber,COUNT(PositionSkillNumber),PlayerNumber," +
			                             "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
                                         "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                         "Skill6,Skill7,Skill8,Skill8,Skill10," +
                                         "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                         "Skill16,Skill17,Skill18,Skill19,Skill20,SpecialTeams " +
                                         "FROM players,positionskills " +
                                         "WHERE WorldNumber = " + leagueNumber + " " +
                                         "AND players.PositionNumber=positionskills.PositionNumber " +
                                         "AND TeamNumber !=0 " +
                                         "GROUP BY PlayerNumber " +
                                         "ORDER BY TeamNumber ASC,PositionNumber ASC");
            int currentTeam=0;
            int currentPosition=0;
            int currentNumOfSkills=0;
            int unSortedPlayers[][]=new int[99][27];
            int unSortedSpecialTeamPlayers[][]=new int[99][2];
            int unSortedKickReturnPlayers[][]=new int[99][2];
            int unSortedPuntReturnPlayers[][]=new int[99][2];
            int currentPlayer=0;
            int currentSpecialTeamsPlayer=0;
            int currentReturnPlayer=0;
            while(queryResult.next())
               {
               int teamNumber=queryResult.getInt(1);
               int positionNumber=queryResult.getInt(2);
               int numOfSkills=queryResult.getInt(3);
               int playerNumber=queryResult.getInt(4);
			   int specialTeams=queryResult.getInt(30);
               if(currentTeam!=teamNumber||currentPosition!=positionNumber)
                 {
                 if(currentTeam!=0)
                   {
                   int[] tempNumOfSkills=new int[currentPosition+1];
                   tempNumOfSkills[currentPosition]=currentNumOfSkills;
                   for(int currentArray=0;currentArray<unSortedPlayers.length;currentArray++)
                      {
                      if(unSortedPlayers[currentArray][1]==0)
                        {
                        unSortedPlayers[currentArray][1]=currentPosition;
                        }
                      }
                   for(int currentSet=0;currentSet<sets.length;currentSet++)
                      {
                      int[] sortSkills=getSortSkill(currentPosition,setNames[currentSet]);	
                      int sortedPlayers[][]=Routines.sortPlayers(currentPosition,
                                                                 unSortedPlayers,
                                                                 tempNumOfSkills,
                                                                 sortSkills[0],
                                                                 sortSkills[1],
                                                                 database,
						                                         context);
                      createDepthCharts(currentTeam,
                                        sets[currentSet],
                                        sortedPlayers,
                                        database);
                      }
                  }
                 if(currentTeam!=teamNumber)
                   {
				   createSpecialTeamsDepthCharts(currentTeam,unSortedSpecialTeamPlayers,35,database);
				   createSpecialTeamsDepthCharts(currentTeam,unSortedPuntReturnPlayers,3,database);
				   createSpecialTeamsDepthCharts(currentTeam,unSortedKickReturnPlayers,4,database);
				   currentSpecialTeamsPlayer=0;
				   currentReturnPlayer=0;
				   unSortedSpecialTeamPlayers=new int[99][2];
				   unSortedPuntReturnPlayers=new int[99][2];
				   unSortedKickReturnPlayers=new int[99][2];
                   Statement sql2=database.createStatement();
                   ResultSet queryResult2;
                   sets=null;
                   setNames=null;
                   if(leagueLevel==3)
                     {
                     queryResult2=sql2.executeQuery("SELECT COUNT(SetNumber),SetNumber,SetName " +
                                                    "FROM sets " +
                                                    "WHERE TeamNumber=" + teamNumber + " " +
                                                    "GROUP BY TeamNumber " +
                                                    "ORDER BY Sequence");
                     if(queryResult2.first())
                       {
                       sets=new int[queryResult2.getInt(1)];
                       setNames=new String[queryResult2.getInt(1)];
                       }
                     }
                   else
                     {
                     queryResult2=sql2.executeQuery("SELECT Sequence,SetNumber,SetName " +
                                                    "FROM defaultsets " +
                                                    "ORDER BY Sequence DESC");
                     if(queryResult2.first())
                       {
                       sets=new int[queryResult2.getInt(1)];
                       setNames=new String[queryResult2.getInt(1)];
                       }
                     queryResult2=sql2.executeQuery("SELECT Sequence,SetNumber,SetName " +
                                                    "FROM defaultsets " +
                                                    "ORDER BY Sequence ASC");
                     }
                   queryResult2.beforeFirst();
                   int currentSet=0;
                   while(queryResult2.next())
                     {
                     sets[currentSet]=queryResult2.getInt(2);
                     setNames[currentSet]=queryResult2.getString(3);
                     currentSet++;
                     }
 				   }
                 currentTeam=teamNumber;
                 currentPosition=positionNumber;
                 currentNumOfSkills=numOfSkills;
                 currentPlayer=0;
                 unSortedPlayers=null;
                 unSortedPlayers=new int[99][27];
                 }
               int skills[]=new int[25];
//               for(int currentSkill=5;currentSkill<(skills.length-5);currentSkill++)
//                  {
//                  skills[currentSkill]=queryResult.getInt(5+currentSkill);
//                  }
  				for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
				   {
				   skills[currentSkill]=queryResult.getInt(currentSkill+1);
				   }
               unSortedPlayers[currentPlayer][0]=playerNumber;
               //SpecialTeamers
               if(specialTeams==1)
                 {
				 unSortedSpecialTeamPlayers[currentSpecialTeamsPlayer][0]=playerNumber;
				 int specialTeamSkill=0;
				 if(currentPosition==15||currentPosition==12||currentPosition==13||
				    currentPosition==17||currentPosition==18||currentPosition==19||
					currentPosition==20)
				    {
				    specialTeamSkill=8;	
				    }
				 if(currentPosition==21||currentPosition==26||currentPosition==27)
					{
					specialTeamSkill=10;	
					}				   
				 if(currentPosition>=7&&currentPosition<=11)
				   {
				   specialTeamSkill=7;	
				   }				   
				 unSortedSpecialTeamPlayers[currentSpecialTeamsPlayer][1]=skills[specialTeamSkill];	
				 currentSpecialTeamsPlayer++; 
                 }
				//Punt&Kick Returners
			  if(currentPosition==13||currentPosition==15||
				currentPosition==21||currentPosition==26||currentPosition==27)
				{
				unSortedPuntReturnPlayers[currentReturnPlayer][0]=playerNumber;
				unSortedKickReturnPlayers[currentReturnPlayer][0]=playerNumber;
				int puntSkill=0;
				int kickSkill=0;
				if(currentPosition==13)
				  {
				  kickSkill=15;
				  puntSkill=17;	
				  }
				if(currentPosition==15)
				  {
				  kickSkill=16;
				  puntSkill=18;
				  }
				if(currentPosition==21||currentPosition==26||currentPosition==27)
				  {
				  kickSkill=13;
				  puntSkill=15;
				  }
				unSortedPuntReturnPlayers[currentReturnPlayer][1]=skills[puntSkill]+skills[puntSkill+1];	
				unSortedKickReturnPlayers[currentReturnPlayer][1]=skills[kickSkill]+skills[kickSkill+1];	
				currentReturnPlayer++; 
				}
			 unSortedPlayers[currentPlayer][1]=positionNumber;
			 for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
				{
				unSortedPlayers[currentPlayer][2+currentSkill]=skills[currentSkill];
				}
			 currentPlayer++;
             }                     
		    createSpecialTeamsDepthCharts(currentTeam,unSortedSpecialTeamPlayers,35,database);
			createSpecialTeamsDepthCharts(currentTeam,unSortedPuntReturnPlayers,3,database);
			createSpecialTeamsDepthCharts(currentTeam,unSortedKickReturnPlayers,4,database);
            int[] tempNumOfSkills=new int[currentPosition+1];
            tempNumOfSkills[currentPosition]=currentNumOfSkills;
            for(int currentArray=0;currentArray<unSortedPlayers.length;currentArray++)
               {
               if(unSortedPlayers[currentArray][1]==0)
                 {
                 unSortedPlayers[currentArray][1]=currentPosition;
                 }
               }
            for(int currentSet=0;currentSet<sets.length;currentSet++)
               {
               int[] sortSkills=getSortSkill(currentPosition,setNames[currentSet]);	
               int sortedPlayers[][]=Routines.sortPlayers(currentPosition,
                                                          unSortedPlayers,
                                                          tempNumOfSkills,
                                                          sortSkills[0],
                                                          sortSkills[1],
                                                          database,
                                                          context);
               createDepthCharts(currentTeam,
                                 sets[currentSet],
                                 sortedPlayers,
                                 database);
               }
            }
         catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error getting teams : " + error,false,context);
            }
          //Tidy database
          try
            {
            Statement sql1=database.createStatement();
            Statement sql2=database.createStatement();
            queryResult=sql1.executeQuery("SELECT TeamNumber " +
                                          "FROM draftboardteam " +
                                          "WHERE LeagueNumber= " + leagueNumber + " " +
                                          "ORDER BY TeamNumber ASC");
            while(queryResult.next())
               {
               int updated=0;
               updated=sql2.executeUpdate("DELETE FROM draftpriorities " +
                                          "WHERE TeamNumber=" + queryResult.getInt(1));
               }
            int updated=0;
			updated=sql2.executeUpdate("DELETE FROM draftratings " +
									   "WHERE LeagueNumber=" + leagueNumber);            
            updated=sql2.executeUpdate("DELETE FROM draftboardteam " +
                                       "WHERE LeagueNumber=" + leagueNumber);
            updated=sql2.executeUpdate("DELETE FROM draftboard " +
                                       "WHERE LeagueNumber=" + leagueNumber);
            }
         catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Error tidying database : " + error,false,context);
            }
          }
        else
          {
          week++;
          boolean postSeason=false;
          if(week>numOfWeeksBeforePostSeason)
            {
            postSeason=true;	
            }
          if(week>numOfWeeks)
            {
			Routines.writeToLog(servletName,"Draft Required",false,context);		
            }
          else
            {
			Routines.writeToLog(servletName,"Week" + week + " Required",false,context);
		    try
			  {
			  sql=database.createStatement();
			  queryResult=sql.executeQuery("SELECT COUNT(FixtureNumber) " +
									       "FROM fixtures " +
										   "WHERE LeagueNumber=" + leagueNumber + " " +
										   "AND Season=" + season + " " +
										   "AND Week=" + week + " " +
										   "GROUP BY LeagueNumber " +
			  							   "ORDER BY FixtureNumber ASC");
			  if(queryResult.first())
			    {
			    numOfGames=queryResult.getInt(1);	
			    }	
			  gameRunners=new GameRunner[numOfGames];
			  threadArray=new Thread[numOfGames];			     		  
			  queryResult=sql.executeQuery("SELECT FixtureNumber,HomeTeam,AwayTeam " +
									       "FROM fixtures " +
										   "WHERE LeagueNumber=" + leagueNumber + " " +
										   "AND Season=" + season + " " +
										   "AND Week=" + week + " " +
										   "ORDER BY FixtureNumber ASC");
			  int currentGame=0;							   
			  while(queryResult.next())
				   {
				   int fixtureNumber=queryResult.getInt(1);	
                   gameRunners[currentGame]=new GameRunner(context,fixtureNumber,leagueNumber,postSeason);
				   threadArray[currentGame]=new Thread(gameRunners[currentGame]);
				   threadArray[currentGame].start();
				   currentGame++;							
				   }
			  }
		   catch(SQLException error)
			  {
			  Routines.writeToLog(servletName,"Error getting fixtures : " + error,false,context);
			  }
		   boolean loop=true;
		   while(loop)
			    {
			    boolean allFinished=true;	
				for(int currentGame=0;currentGame<gameRunners.length;currentGame++)
				   {
				   if(gameRunners[currentGame]!=null)
					 {
					 if(!gameRunners[currentGame].stillActive())
					   {
					   gameRunners[currentGame]=null;
					   threadArray[currentGame]=null;
					   }
					 else
					   {
					   allFinished=false;	  
					   }
				     }
				   }
				if(allFinished)
				  {
				  loop=false;   
				  }
				else
				  { 
				  try
				     {
				     Thread.sleep(60*1000);
				     }
				  catch(InterruptedException error)
				     {
				     Routines.writeToLog(servletName,"InterruptError : " + error,false,context);
				     }
				  }      
			    }   	  
            }
          }
        Routines.advanceLeague(leagueNumber,context,database);
        Routines.writeToLog(servletName,"Turn Ended",false,context);
		System.out.println("Turn Ended");
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Error retrieving league("+leagueNumber+") : " + error,false,context);
        }
      try
        {
        Statement sql=database.createStatement();
        int updated=sql.executeUpdate("UPDATE leagues " +
                                      "SET LockDown=0 " +
                                      "WHERE LeagueNumber="+leagueNumber);
        if(updated!=1)
          {
          Routines.writeToLog(servletName,"Unable to unlock League("+leagueNumber+")",false,context);
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Error unlocking League("+leagueNumber+") : " + error,false,context);
        }
      pool.debug(context);  
      pool.returnConnection(database);
      database=null;  
      active=false;
      }

   private int[] getSortSkill(int positionNumber,
                              String setName)
      {
      int[] returnInt=new int[2];
      returnInt[0]=1;
      returnInt[1]=1;
	  if(setName.equals("Fast"))
		{
		//OL
		if(positionNumber>=7&&positionNumber<=11)
		  {
          returnInt[1]=5;	
		  }
		//TE
		if(positionNumber==12)
		  {
		  returnInt[1]=4;	
		  }
        //WR
	   	if(positionNumber==13)
		  {
          returnInt[1]=4;	
		  }  		  
		//RB
		if(positionNumber==15||positionNumber==16)
		  {
          returnInt[1]=5;	
		  }		  
        //DE
		if(positionNumber==17)
		  {
		  returnInt[1]=4;	
		  }  
		//DT
		if(positionNumber==18)
		  {
		  returnInt[1]=5;	
		  }		  
		//LB
		if(positionNumber==19||positionNumber==20)
		  {
		  returnInt[1]=5;	
		  }		  
		//DB
		if(positionNumber==21||positionNumber==26||positionNumber==27)
		  {
          returnInt[1]=4;	  
		  }
		}
	  if(setName.equals("Hands"))
		{
		//OL
		if(positionNumber>=7&&positionNumber<=11)
		  {
		  returnInt[1]=4;	
		  }
		//TE
		if(positionNumber==12)
		  {
		  returnInt[0]=2;	
		  returnInt[1]=5;	
		  }
		//WR
		if(positionNumber==13)
		  {
		  returnInt[0]=3;	
		  returnInt[1]=2;	
		  }  		  
		//RB
		if(positionNumber==15||positionNumber==16)
		  {
		  returnInt[0]=4;	
		  returnInt[1]=1;	
		  }		  
		//DE
		if(positionNumber==17)
		  {
		  returnInt[1]=4;	
		  }  
		//DT
		if(positionNumber==18)
		  {
		  returnInt[1]=5;	
		  }		  
		//LB
		if(positionNumber==19||positionNumber==20)
		  {
		  returnInt[1]=5;	
		  }		  
		//DB
		if(positionNumber==21||positionNumber==26||positionNumber==27)
		  {
		  returnInt[1]=4;	  
		  }
		}
	  if(setName.equals("Heavy"))
		{
		//OL
		if(positionNumber>=7&&positionNumber<=11)
		  {
		  returnInt[1]=4;	
		  }
		//TE
		if(positionNumber==12)
		  {
		  returnInt[1]=5;	
		  }
		//WR
		if(positionNumber==13)
		  {
		  returnInt[1]=5;	
		  }  		  
		//RB
		if(positionNumber==15||positionNumber==16)
		  {
		  returnInt[0]=2;	
		  returnInt[1]=1;	
		  }		  
		//DE
		if(positionNumber==17)
		  {
		  returnInt[1]=5;	
		  }  
		//DT
		if(positionNumber==18)
		  {
		  returnInt[1]=4;	
		  }		  
		//LB
		if(positionNumber==19||positionNumber==20)
		  {
		  returnInt[1]=4;	
		  }		  
		//DB
		if(positionNumber==21||positionNumber==26||positionNumber==27)
		  {
		  returnInt[1]=5;	  
		  }			
		}        
      return returnInt;                           
      }

   private void createDepthCharts(int teamNumber,
                                  int setNumber,
                                  int[][] sortedPlayers,
                                  Connection database)
      {
      int positionNumber=sortedPlayers[0][1];
      int positionDepth=1;
      for(int currentPlayer=0;currentPlayer<sortedPlayers.length;currentPlayer++)
         {
         int playerNumber=sortedPlayers[currentPlayer][0];
         if(playerNumber!=0)
           {
           try
             {
             Statement sql=database.createStatement();
             int updated=sql.executeUpdate("INSERT INTO depthcharts " +
                                           "(TeamNumber,PlayerNumber,PositionNumber,Depth,SetNumber,DateTimeStamp)" +
                                           " VALUES (" +
                                           teamNumber + "," +
                                           playerNumber + "," +
                                           positionNumber + "," +
                                           positionDepth + "," +
                                           setNumber + ",'" +
                                           Routines.getDateTime(false) +
                                           "')");

              if(updated!=1)
                {
                Routines.writeToLog(servletName,"depthcharts("+teamNumber+"/"+positionNumber+") not created",false,context);
                }
				if(positionNumber==15)
				   {
				   //Double each RB depth chart position for FB
				   sql=database.createStatement();
				   updated=sql.executeUpdate("INSERT INTO depthcharts " +
											 "(TeamNumber,PlayerNumber,PositionNumber,Depth,SetNumber,DateTimeStamp)" +
											 " VALUES (" +
											 teamNumber + "," +
											 playerNumber + "," +
											 "16" + "," +
											 positionDepth + "," +
											 setNumber + ",'" +
											 Routines.getDateTime(false) +
											 "')");

					 if(updated!=1)
					   {
					   Routines.writeToLog(servletName,"depthcharts("+teamNumber+"/"+"16"+") not created",false,context);
					   }                	                             
				   }                
              }
           catch(SQLException error)
              {
              Routines.writeToLog(servletName,"Error setting depthcharts("+teamNumber+"/"+positionNumber+") : " + error,false,context);
              }
           }
         positionDepth++;
         }
      }

	private void createSpecialTeamsDepthCharts(int teamNumber,
								               int[][] unSortedSpecialTeamPlayers,
								               int positionNumber,
								               Connection database)
	   {
	   String specialTeamsPlayers[]=new String[unSortedSpecialTeamPlayers.length];         
	   for(int currentSpecialTeamPlayer=0;currentSpecialTeamPlayer<unSortedSpecialTeamPlayers.length;currentSpecialTeamPlayer++)
	      {
	      int skill=unSortedSpecialTeamPlayers[currentSpecialTeamPlayer][1];
	      int player=unSortedSpecialTeamPlayers[currentSpecialTeamPlayer][0];
	      String playerPadding="";
	      switch(String.valueOf(player).length())
	        {
	        case 0:
	          playerPadding="0000000000";
	          break;	
			case 1:
			  playerPadding="000000000";
			  break;
			case 2:
			  playerPadding="00000000";
			  break;
			case 3:
			  playerPadding="0000000";
			  break;
			case 4:
			  playerPadding="000000";
			  break;
			case 5:
			  playerPadding="00000";
			  break;
			case 6:
			  playerPadding="0000";
			  break;
			case 7:
			  playerPadding="000";
			  break;
			case 8:
			  playerPadding="00";
			  break;
			case 9:
			  playerPadding="0";
			  break;
	        }
	      if(player>0)
		    {	
		    switch((String.valueOf(skill)).length())
			      {
  			      case 0:
				     specialTeamsPlayers[currentSpecialTeamPlayer]="000"+playerPadding+player;
				     break;
			      case 1:
				     specialTeamsPlayers[currentSpecialTeamPlayer]="00"+skill+playerPadding+player;
				     break;
			      case 2:
				     specialTeamsPlayers[currentSpecialTeamPlayer]="0"+skill+playerPadding+player;
				     break;
			      case 3:
				     specialTeamsPlayers[currentSpecialTeamPlayer]=String.valueOf(skill)+playerPadding+player;
				     break;
			      default:
				     System.out.println("Unable to pad special teams skill");
			      }
		    }
	      else
		    { 
		    specialTeamsPlayers[currentSpecialTeamPlayer]="";	   					  	
		    }
	      }
	   Arrays.sort(specialTeamsPlayers);
	   int[][] sortedSpecialTeamsPlayers=new int[99][2];
	   int arrayLength=sortedSpecialTeamsPlayers.length-1;
	   for(int currentSpecialTeamsPlayer=arrayLength;currentSpecialTeamsPlayer>=0;currentSpecialTeamsPlayer--)
	      {
	      if(specialTeamsPlayers[currentSpecialTeamsPlayer].length()>0)
		    {	
		    int specialTeamsPlayerNumber=Routines.safeParseInt(specialTeamsPlayers[currentSpecialTeamsPlayer].substring(3,13));
		    sortedSpecialTeamsPlayers[arrayLength-currentSpecialTeamsPlayer][0]=specialTeamsPlayerNumber;
		    sortedSpecialTeamsPlayers[arrayLength-currentSpecialTeamsPlayer][1]=positionNumber;
		    }
	      }	
	    createDepthCharts(teamNumber,
					      0,
					      sortedSpecialTeamsPlayers,
					      database);
	    }

   public boolean stillActive()
      {
      return active;
      }
      
	public static int totalNumberOfLines()
	   {
	   return totalNumberOfLines;
	   }      

    public static String[] projectFiles()
       {
       return list;	
       }

	class DirectoryFilter implements FilenameFilter 
	  {
	  String wildcard;
	  
	  DirectoryFilter(String wildcard)
	     {
	     this.wildcard = wildcard;
	     }
	     
	  public boolean accept(File directory,
	                        String name) 
	    {
		String files = new File(name).getName();
		return files.indexOf(wildcard) != -1;
	    }
	  }

    }
