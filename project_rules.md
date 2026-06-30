Project Proposal
Project Title: MUST-CONNECT
Introduction:
In modern universities, effective academic communication between students, teachers,
and administration is a critical challenge. In the Department of Computer Science and
Information Technology (CS & IT), the flow of official information such as timetables,
challans, holidays, exam schedules, and lecture materials is often fragmented across
notice boards, informal messaging applications, and verbal announcements. Additionally,
direct and unmonitored communication between teachers and students is discouraged,
making it difficult to maintain discipline, transparency, and record keeping. As a result,
students frequently miss important updates, while teachers struggle to manage class-level
communication efficiently. This project proposes a centralized, role-based digital
platform that ensures secure, monitored, and structured communication within the CS &
IT department.
Existing System/ Description of the Current Situation:
Currently, academic communication in the CS & IT department relies on traditional
notice boards, WhatsApp groups, emails, and verbal instructions. These methods are
unstructured, unreliable, and difficult to monitor. Multiple WhatsApp groups create
confusion, official documents get lost in chat histories, and there is no central system to
verify the authenticity of shared information. Teachers often manage several classes
manually, while students depend on classmates for updates. There is no proper
mechanism to archive announcements, control access, or ensure that communication
follows departmental rules. This fragmented approach results in miscommunication,
missed deadlines, and administrative inefficiency.
Problem Statement:
The absence of a centralized and monitored communication platform in the CS & IT
department leads to inefficiency, misinformation, and lack of accountability. Students
miss critical academic updates such as challans, exam schedules, and holidays. Teachers
lack an organized system to manage class announcements and share lecture materials.
Informal communication channels cannot be monitored, posing risks of unauthorized
information sharing and policy violations. Furthermore, there is no role-based access
control, making it difficult to separate administrative, academic, and student-level
communication. These issues highlight the need for a secure, structured, and departmentspecific solution.
Proposed Solution:
To address these challenges, this project proposes MUST-CONNECT, a role-based
departmental communication and classroom management system designed exclusively
for the CS & IT department. The system introduces four user roles: Super Admin,
Department Admin, Teachers, and Students. Department Admins will manage official
announcements through a General Feed, while teachers will communicate with students
through announcement-only class groups. Students will have read-only access to class
groups and official feeds, ensuring discipline and clarity. All communication will be
monitored, archived, and controlled through permissions. This solution ensures secure
information delivery, reduces miscommunication, and improves overall academic
coordination within the department.
Scope of the Project:
User Management Module
This module manages all users of the system including Super Admin, Department
Admin, Teachers, and Students. Each user is assigned a role with specific permissions.
Secure authentication and password management are implemented to ensure data
protection and controlled access.
Department Administration Module
This module allows the Department Admin to manage classes, assign teachers and
students, upload timetables, challans, and official documents, and post announcements in
the General Feed. It serves as the administrative backbone of the system.
Class Group Communication Module
Each class has a dedicated announcement-only group where teachers can post lectures,
files, images, PDFs, and polls. Students can only view content and communicate
privately with all class members (students and teachers) through monitored direct messages.
General Feed Module
The General Feed acts as an official notice board for the CS & IT department. Only the
Department Admin can post announcements, while students and teachers can view them.
Super Admin has authority to monitor and delete any post.
Monitoring & Security Module
This module ensures that all communication within the system is logged and monitored.
Password reset controls, role-based permissions, and supervised chat access help
maintain discipline and compliance with university policies.
SYSTEM Block diagram
How the CS & IT Department App Works
Login & Role Selection
When users launch the app, they log in with their credentials. The system recognizes their
role:
Super Admin
Department Admin
Teacher
Student
Super Admin Workflow
Manages the Department Admin
The Super Admin oversees who the Department Admin is and can update or replace
them.
Monitors the General Feed
They can view and delete any posts but cannot create posts themselves.
Monitors All Chats
They can review all class group messages and private DMs for compliance.
Resets Any Password
They can reset passwords for any user (Dept Admin, Teacher, Student) if needed.
Department Admin Workflow
Creates & Assigns Classes/Groups
The Department Admin sets up subjects/classes and assigns teachers and students,
creating dedicated class groups automatically.
Uploads Timetables, Notices, Documents
Official departmental documents and schedules are uploaded by Department Admin for
all relevant users.
Posts/Edits in the General Feed
The only user who can add or edit official news and updates in the General Feed for the
department.
Resets Teacher/Student Passwords
Has the ability to help teachers or students recover access by resetting their passwords.
Teacher Workflow
Sees Assigned Classes
Teachers see only the classes they are responsible for.
Posts Notes, Files to Class Group
They can upload lecture notes, PDFs, images, and make announcements to their assigned
groups.
Views Class Student List
Teachers get a roster of all students in their class groups.
Direct Messages Students
They can have one-on-one conversations with any student from their class groups.
Change Own Password
Security feature to keep their account protected.
Student Workflow
Views General Feed (Read Only)
Students can see all official updates but cannot post or comment.
Accesses Assigned Class Groups
Students are automatically added to class groups by the system and can only view
announcements and files.
Downloads Notes/Files
Can download timetables, notes, PDFs and other shared materials from their class groups
or the uploads section.
Direct Messages Class Members
For asking questions or clarifications privately with teachers and fellow students.
Change Own Password
To keep their account secure.
Core App Modules
General Feed: Official department-wide notice board (posting by Dept Admin only, readonly for others).
Class Groups: One-way announcement groups for each class (teachers post, students
view).
Documents/Notices: Section for timetables, exam forms, holiday announcements, etc.
Chat/DMs: Internal chat for secure communication between all class members, monitored by Super
Admin.
Password Management: Hierarchical controls for password resets and chang