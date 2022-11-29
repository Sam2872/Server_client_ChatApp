package task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Client {
	static Boolean check = false;
	static Boolean x = false;
	static Boolean y = false;
	static Boolean z = false;
	static String temp = "";
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress ip = InetAddress.getLocalHost();
		int port = 9995;
		Scanner sc = new Scanner(System.in);
		Socket s = new Socket(ip, port);
		String UserInput;
		String Current_Group;
		String userName = "";
		

		DataOutputStream os = new DataOutputStream(s.getOutputStream());
		DataInputStream is = new DataInputStream(s.getInputStream());

		Thread ReadMsg = new Thread(new Runnable() {
			
			public void run() {
				while (true) {
					try {
						String response = is.readUTF();
//						if (response.contains(":")) {
//							StringTokenizer st = new StringTokenizer(response, " : ");
//							os.writeUTF("ack#" + st.nextToken());
//						}
						if(response.contains("join?!")) {
							StringTokenizer st = new StringTokenizer(response,":");
							temp = st.nextToken();
						}
						if(response.contains("#Accepted")) {
							StringTokenizer st = new StringTokenizer(response,":");
							String user = "";
							while(st.hasMoreElements()) {
								user = st.nextToken();
							} 
							os.writeUTF("#AddUser :#" + user);
						} 
						if(response.contains("rejected")) {
							y = false;
						}
						if (response.equals("No Clients Available") || response.equals("No such Client")||response.equals("No such Group")||response.equals("User not found")||response.contains("UserName already exists")||response.contains("Group already exists"))
							check = true;
						System.out.println(response);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		ReadMsg.start();
		
//		User Login
		while(true) {
		System.out.println("1.Existing User\n2. New User");
		String option = sc.nextLine();
		if(option.equals("1")) {
			while(true) {
			System.out.println("Hello there! Enter your name to Login:");
			String name = sc.nextLine();
			if(name.equals("exit")) break;
			os.writeUTF("CheckClient:"+name);
			Thread.sleep(50);
			if(check == true) {
				check  = false;
				continue;
			}
			userName = name;
			System.out.println("#1. Chat \n#2. Group Chat \n#3. Logout");break;
			}
			break;
		}
		else if(option.equals("2")) {
			System.out.println("Hello there! Enter your name:");
			while(true){
			String name = sc.nextLine();
			os.writeUTF("Client__Name:"+name);Thread.sleep(50);
			if(check == true) {
				check  = false;
				continue;
			}
			userName = name;
			System.out.println("#1. Chat \n#2. Group Chat \n#3. Logout");
			break;
			}
			break;
		}
		else {
			System.out.println("Enter a valid option");
		}
		}
		
		
		
		
		while (true) {

			UserInput = sc.nextLine();
			
			if(UserInput.equals("Yes*")) {
				os.writeUTF("#ok:"+temp);
			}
			
			if(UserInput.equals("No*")) {
				os.writeUTF("#Rejected:"+temp);
			}
			
			if (x == true) {
				if (UserInput.equals("exit")) {
					os.writeUTF("#disconnected:"+userName);
					x = false;
					continue;
				}
				os.writeUTF("message :" + UserInput);

			}
			
			if (y == true) {
				if (UserInput.equals("done")) {
					System.out.println("Users added");
					y = false;
					continue;
				}
				os.writeUTF("#AddUser :#" + UserInput);
				
			} 
			
			if (z == true) {
				if (UserInput.equals("exit") || UserInput.equals("done")) {
					os.writeUTF("#disconnectedGrp:"+userName);
					z = false;
					continue;
				}
				
				if(UserInput.contains("#Add>")){ 
					StringTokenizer st = new StringTokenizer(UserInput,">");
					String newClient = "";
					while(st.hasMoreElements()) {
						newClient = st.nextToken();
					}
					os.writeUTF("#AddUser :#"+newClient);
					continue;
				}
				
				os.writeUTF("GrpMessage :" + UserInput);
				
			}
			
			if(UserInput.equals("home")|| UserInput.equals("exit")) {
				System.out.println("Welcome User!..\n#1. Chat \n#2. Group Chat \n#3. Logout");
			}

			if (UserInput.equals("#1")) {
				os.writeUTF("#GetUsers");
				Thread.sleep(50);
				if (check == true) {
					check = false;
					continue;
				}
				System.out.println("Enter [Select> \"Client name\"] to chat with a client");
			}

			if (UserInput.contains("Select>")) {
				StringTokenizer st = new StringTokenizer(UserInput, ">");
				String endClient = "";
				while (st.hasMoreElements()) {
					endClient = st.nextToken();
				}
				os.writeUTF("clientName :" + endClient);
				Thread.sleep(50);
				if(check == true) {
					check = false;
					continue;
				}
				x = true;
			}

			if (UserInput.equals("#2")) {
				os.writeUTF("#showGroups");
				Thread.sleep(50);
				System.out.println("\n *Enter Group> \"Group name\" to select group \n *Enter Create> \"Group Name\" to Create Group");
			}
			
			
			if(UserInput.contains("Create>")) {
				while(true) {
				StringTokenizer st = new StringTokenizer(UserInput, ">");
				String newGroup = "";
				while (st.hasMoreElements()) {
					newGroup = st.nextToken();
				}
				Current_Group = newGroup;
				os.writeUTF("#CreateGroup :" +newGroup);
				if(check == true) {
					check = false;
					continue;
				}
				break;
				}
				System.out.println("Add Users");
				y = true;
			}
			
			if(UserInput.contains("Group>")) {
				StringTokenizer st = new StringTokenizer(UserInput,">");
				String GroupName = "";
				while(st.hasMoreElements()) {
					GroupName = st.nextToken();
				}
				os.writeUTF("GroupName :" + GroupName);
				Thread.sleep(50);
				if(check == true) {
					check = false;
					continue;
				}
 				Current_Group = GroupName;
				z = true;
			}
			
			if(UserInput.equals("#Add")) {
				os.writeUTF(UserInput);
			}
			
	

			
			if (UserInput.equals("#3")) {
				System.out.println("Logged-Out");
				os.writeUTF("#logout");
				s.close();
				System.exit(0);
			}

		}

	}
}




