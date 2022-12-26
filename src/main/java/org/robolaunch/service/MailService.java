package org.robolaunch.service;

import java.util.ArrayList;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.models.CurrentUser;
import org.robolaunch.models.InvitedUser;
import org.robolaunch.models.MailModel;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import io.quarkus.arc.log.LoggerName;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MailService {
    @Inject
    Logger log;

    @Inject
    Mailer reactiveMailer;

    @Inject
    UserRepository userRepository;

    @Inject
    DepartmentService departmentService;

    @Inject
    GroupAdminRepository groupAdminRepository;

    @LoggerName("mailLogger")
    Logger mailLogger;

    @ConfigProperty(name = "frontend.url")
    String frontendUrl;

    public void sendMail(MailModel mail) {
        reactiveMailer
                .send(Mail.withHtml(mail.getTo(), mail.getSubject(),
                        mail.getMessage()));
    }

    public void sendPasswordMail(InvitedUser user, String password) {
        MailModel mail = new MailModel();
        mail.setTo(user.getEmail());
        mail.setSubject("Your Robolaunch Password");
        String body = "<p>Hello " + user.getUsername()
                + ", welcome to Robolaunch! Your first password is given below. Please change it after your first login.</p>"
                + "\n" +
                "<p>Password: <b>" + password + "</b>" + "</p>" + "\n" +
                "<p><a href=\"" + frontendUrl + "/login\">" + "Click here" + "</a> to go login page.</p>";
        mail.setMessage(body);
        try {
            sendMail(mail);
            mailLogger.info("Password mail sent to " + user.getEmail());
        } catch (Exception e) {
            mailLogger.error("Failed to send password mail to " + user.getEmail());
        }
    }

    public void sendInvitedInformativeMail(InvitedUser user, Organization organization) {
        MailModel mail = new MailModel();
        mail.setTo(user.getEmail());
        mail.setSubject("Robolaunch Organization Update");
        String body = "<p>Hello " + user.getUsername()
                + ", Welcome to Robolaunch! Someone added to you an organization!</p>"
                + "\n" +
                "<p>You have been added to organization: " + organization.getName();
        mail.setMessage(body);
        try {
            sendMail(mail);
            mailLogger.info("Invited informative mail sent to " + user.getEmail());
        } catch (Exception e) {
            mailLogger.error("Failed to send invited informative mail to " + user.getEmail());
        }
    }

    public Response sendInvitedUserMail(String token, String email, Organization organization) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(email);
            mail.setSubject("Robolaunch Organization Invitation");
            String body = "<p>Hello, Welcome to Robolaunch!"
                    + "You are invited to organization " + organization.getName() + ".</p>"
                    + "\n" +
                    "<p><a href=\"" + frontendUrl + "/invited-user?organization=" + organization.getName() + "&email="
                    + email + "&token="
                    + token + "\">" + "Click here" + "</a> to go registration page.</p>";
            mail.setMessage(body);
            sendMail(mail);
            mailLogger.info("Invited user mail sent to " + email);

            return new Response(true, UUID.randomUUID().toString());
        } catch (Exception e) {
            mailLogger.error("Failed to send invited user mail to " + email);
            return new Response(false, UUID.randomUUID().toString());

        }
    }

    public void sendMailToManagersOperation(Organization organization, String teamId, String cloudInstanceName,
            String operation) {
        try {
            System.out.println("TeamID " + teamId);
            MailModel mail = new MailModel();
            ArrayList<User> teamManagers = departmentService.getTeamManagers(organization, teamId);
            String teamName = groupAdminRepository.getGroupDescription(teamId);
            System.out.println("Team name: " + teamName);
            for (User user : teamManagers) {
                mail.setTo(user.getEmail());
                mail.setSubject("Robolaunch Robotics Cloud Instance " + operation);
                String body = "<p>Hello " + user.getUsername()
                        + ",\n Robotics Cloud " + "<b>" + cloudInstanceName + "</b>" + " for team " + "<b>" + teamName
                        + "</b>" + " has been " + operation + ".</p>"
                        + "\n" +
                        "You are receiving this email because you are one of the managers of the team.";
                mail.setMessage(body);
                sendMail(mail);
                mailLogger.info("Informative mail sent to " + user.getEmail());
            }
            mailLogger.info("Informative mail sent to all managers.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            mailLogger.error("Failed to send mail.");
        }
    }

    public Response sendInvitedUserAcceptanceMail(String token, String email, Organization organization) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(email);
            mail.setSubject("Robolaunch Organization Invitation");
            String body = "<p>Hello!"
                    + "You are invited to organization " + organization.getName() + ".</p>"
                    + "\n" +
                    "<p><a href=\"" + frontendUrl + "/invite-organization?organization=" + organization.getName()
                    + "&email="
                    + email + "&token="
                    + token + "\">" + "Click here" + "</a> to see the invitation.</p>";
            mail.setMessage(body);
            sendMail(mail);
            mailLogger.info("Invited user acceptance mail sent to " + email);
            return new Response(true, UUID.randomUUID().toString());
        } catch (Exception e) {
            mailLogger.error("Failed to send invited user acceptance mail to " + email);
            return new Response(false, UUID.randomUUID().toString());

        }
    }

    public void sendAcceptedOrganizationMail(CurrentUser currentUser) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(currentUser.getEmail());
            mail.setSubject("Robolaunch Enterprise Organization Application");
            String body = "<p>Hello! "
                    + "We are happy to announce that your application for Enterprise Organization has been accepted. You can now login to our application and start to use enterprise functionalities!"
                    + ".</p>"
                    + "\n" +
                    "<p><a href=\"" + frontendUrl + "/login\">" + "Click here"
                    + "</a> to go to Robolaunch.</p>";
            mail.setMessage(body);
            sendMail(mail);
            mailLogger.info("Accepted organization mail sent.");
        } catch (Exception e) {
            mailLogger.error("Failed to send rejected organization mail");
        }
    }

    public void sendRejectedOrganizationMail(CurrentUser currentUser) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(currentUser.getEmail());
            mail.setSubject("Robolaunch Enterprise Organization Application");
            String body = "<p>Hello!"
                    + "We are sending this email to inform you that your application for Enterprise Organization has been rejected. Please contact us for more information."
                    + ".</p>"
                    + "\n" +
                    "<p><a href=\"" + frontendUrl + "/login\">" + "Click here"
                    + "</a> to go to Robolaunch.</p>";
            mail.setMessage(body);
            sendMail(mail);
            mailLogger.info("Rejected organization mail sent.");
        } catch (Exception e) {
            mailLogger.error("Failed to send rejected organization mail");
        }
    }

}
