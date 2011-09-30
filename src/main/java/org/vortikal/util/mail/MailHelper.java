package org.vortikal.util.mail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

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
            String siteName, String uri, String title, String[] mailMultipleTo, String emailFrom,
            String comment, String subject) throws Exception {
        
        String mailBody = mailTemplateProvider.generateMailBody(title, uri, emailFrom, comment, siteName);

        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setSubject(subject);
        helper.setFrom(emailFrom);
        helper.setTo(mailMultipleTo);
        helper.setText(mailBody, true); // send HTML

        return mimeMessage;
    }
}
