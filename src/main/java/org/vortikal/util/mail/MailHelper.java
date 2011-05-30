package org.vortikal.util.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class MailHelper {

    public static boolean isValidEmail(String[] addrs) {
        for (String addr : addrs) {
            if (!isValidEmail(addr)) {
                return false;
            }
        }
        return true;
    }


    public static boolean isValidEmail(String addr) {
        if (org.springframework.util.StringUtils.countOccurrencesOf(addr, "@") == 0) {
            return false;
        }
        try {
            new InternetAddress(addr);
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
    

    public static MimeMessage createMimeMessage(JavaMailSenderImpl sender, MailTemplateProvider mailTemplateProvider,
            Service viewService, String siteName, Resource resource, String[] mailMultipleTo, String emailFrom,
            String comment, String subject) throws Exception {

        URL url = viewService.constructURL(resource.getURI());

        String mailBody = mailTemplateProvider.generateMailBody(resource.getTitle(), url, emailFrom, comment, siteName);

        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setSubject(subject);
        helper.setFrom(emailFrom);
        helper.setTo(mailMultipleTo);
        // HTML (TRUE | FALSE)
        helper.setText(mailBody, true);

        return mimeMessage;
    }
}
