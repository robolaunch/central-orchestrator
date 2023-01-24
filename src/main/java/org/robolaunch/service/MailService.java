package org.robolaunch.service;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.model.account.MailModel;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.User;
import org.robolaunch.model.response.PlainResponse;
import org.robolaunch.repository.abstracts.AccountRepository;

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
    TeamService departmentService;

    @Inject
    AccountRepository accountRepository;

    @LoggerName("mailLogger")
    Logger mailLogger;

    @ConfigProperty(name = "frontend.url")
    String frontendUrl;

    public void sendMail(MailModel mail) {
        reactiveMailer
                .send(Mail.withHtml(mail.getTo(), mail.getSubject(),
                        mail.getMessage()));
    }

    public void sendMailToManagersOperation(Organization organization, String teamId, String cloudInstanceName,
            String operation) {
        try {
            MailModel mail = new MailModel();
            ArrayList<User> teamManagers = departmentService.getTeamManagers(organization, teamId);
            String teamName = accountRepository.getGroupDescription(teamId);
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
            mailLogger.error("Failed to send mail.");
        }
    }

    public PlainResponse sendInvitedUserAcceptanceMail(String token, String email, Organization organization) {
        PlainResponse plainResponse = new PlainResponse();
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
            plainResponse.setSuccess(true);
            plainResponse.setMessage("Invited user acceptance mail sent to " + email);
        } catch (Exception e) {
            mailLogger.error("Failed to send invited user acceptance mail to " + email);
            plainResponse.setSuccess(false);
            plainResponse.setMessage("Failed to send invited user acceptance mail to " + email);
        }
        return plainResponse;
    }

    public void sendAcceptedOrganizationMail(User user) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(user.getEmail());
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

    public void sendRejectedOrganizationMail(User user) {
        try {
            MailModel mail = new MailModel();
            mail.setTo(user.getEmail());
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
