package com.demo.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.InputStream;
import java.util.Properties;

@WebServlet("/insurance")
public class UserServlet extends HttpServlet {
    
    // Using vulnerable Log4j 2.14.1 (CVE-2021-44228)
    private static final Logger logger = LogManager.getLogger(UserServlet.class);
    
    // Method to detect Log4j version
    private String getLog4jVersion() {
        try {
            // Try to get version from Log4j itself
            Package pkg = LogManager.class.getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                return pkg.getImplementationVersion();
            }
            
            // Fallback: try to read from properties
            InputStream is = LogManager.class.getResourceAsStream("/META-INF/maven/org.apache.logging.log4j/log4j-core/pom.properties");
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("version");
                is.close();
                if (version != null) {
                    return version;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "2.14.1"; // Default fallback
    }
    
    // Check if Log4j version is vulnerable to CVE-2021-44228
    private boolean isLog4jVulnerable(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            
            // Vulnerable if version < 2.17.0
            if (major < 2) return true;
            if (major == 2 && minor < 17) return true;
            return false;
        } catch (Exception e) {
            return true; // Assume vulnerable if can't parse
        }
    }
    
    // Check if input contains Log4Shell attack pattern
    private boolean isLog4ShellAttempt(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("${jndi:") || lower.contains("$%7bjndi:");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        String patientId = request.getParameter("patientId");
        String log4jVersion = getLog4jVersion();
        boolean isVulnerable = isLog4jVulnerable(log4jVersion);
        boolean isAttackAttempt = isLog4ShellAttempt(patientId);
        
        // Log user input - VULNERABLE to Log4Shell if malicious JNDI lookup is provided
        logger.info("Insurance lookup request for patient ID: " + patientId);
        
        out.println("<html><head><title>Patient Insurance Lookup</title>");
        out.println("<link rel='stylesheet' type='text/css' href='styles.css'></head>");
        out.println("<body>");
        out.println("<div class='container'>");
        
        // Show security banner if Log4Shell attack is attempted
        if (isAttackAttempt) {
            if (isVulnerable) {
                out.println("<div class='security-banner vulnerable'>");
                out.println("<span class='banner-icon'>🚨</span>");
                out.println("<span class='banner-text'>CRITICAL: Log4Shell Attack Detected! System is VULNERABLE (Log4j " + log4jVersion + ")</span>");
                out.println("</div>");
            } else {
                out.println("<div class='security-banner secure'>");
                out.println("<span class='banner-icon'>🛡️</span>");
                out.println("<span class='banner-text'>Log4Shell Attack Blocked! System is PROTECTED (Log4j " + log4jVersion + ")</span>");
                out.println("</div>");
            }
        }
        
        out.println("<h1>🏥 Patient Insurance Lookup</h1>");
        
        if (patientId != null && !patientId.isEmpty()) {
            try {
                // VULNERABILITY: SQL Injection - user input directly concatenated into query
                String query = "SELECT patient_name, insurance_provider, policy_number, ssn, medical_history " +
                              "FROM patient_insurance WHERE patient_id = '" + patientId + "'";
                
                out.println("<div class='result'>");
                out.println("<h2>Database Query:</h2>");
                out.println("<p class='query'>" + query + "</p>");
                out.println("<p class='warning'>⚠️ This query is vulnerable to SQL injection!</p>");
                out.println("<p class='warning'>⚠️ Sensitive PHI (Protected Health Information) at risk!</p>");
                out.println("<p class='info'>Searching for Patient ID: <strong>" + patientId + "</strong></p>");
                out.println("<div class='phi-warning'>");
                out.println("<h3>⚕️ HIPAA Violation Risk</h3>");
                out.println("<p>This query could expose:</p>");
                out.println("<ul>");
                out.println("<li>Patient Names</li>");
                out.println("<li>Insurance Policy Numbers</li>");
                out.println("<li>Social Security Numbers</li>");
                out.println("<li>Medical History Records</li>");
                out.println("</ul>");
                out.println("</div>");
                out.println("</div>");
                
            } catch (Exception e) {
                logger.error("Error processing insurance lookup", e);
                out.println("<p class='error'>Error: " + e.getMessage() + "</p>");
            }
        } else {
            out.println("<p class='info'>Please provide a patient ID parameter</p>");
            out.println("<p class='example'>Example: /insurance?patientId=P12345</p>");
        }
        
        out.println("<div class='vulnerabilities'>");
        out.println("<h2>Known Vulnerabilities in this Healthcare Application:</h2>");
        out.println("<ul>");
        out.println("<li><strong>SQL Injection:</strong> Patient ID is directly concatenated into SQL query - could expose entire patient database</li>");
        
        // Show Log4j status dynamically
        if (isVulnerable) {
            out.println("<li class='vuln-item vulnerable'><strong>Log4Shell (CVE-2021-44228):</strong> Using vulnerable Log4j " + log4jVersion + " - allows remote code execution ⚠️</li>");
        } else {
            out.println("<li class='vuln-item fixed'><strong>Log4Shell (CVE-2021-44228):</strong> ✅ FIXED - Using secure Log4j " + log4jVersion + "</li>");
        }
        
        out.println("<li><strong>HIPAA Compliance Risk:</strong> Inadequate protection of Protected Health Information (PHI)</li>");
        out.println("</ul>");
        out.println("</div>");
        
        // Show additional security info if Log4Shell attack was attempted
        if (isAttackAttempt) {
            out.println("<div class='attack-analysis'>");
            out.println("<h2>🔍 Attack Analysis</h2>");
            if (isVulnerable) {
                out.println("<div class='attack-result vulnerable'>");
                out.println("<h3>⚠️ SYSTEM VULNERABLE</h3>");
                out.println("<p>The Log4Shell payload was logged by the vulnerable Log4j " + log4jVersion + " library.</p>");
                out.println("<p><strong>Attack Vector:</strong> <code>" + patientId + "</code></p>");
                out.println("<ul>");
                out.println("<li>🚨 JNDI lookup would be processed</li>");
                out.println("<li>🚨 Remote code execution possible</li>");
                out.println("<li>🚨 Attacker could gain full system access</li>");
                out.println("<li>🚨 Patient database at risk of theft</li>");
                out.println("</ul>");
                out.println("<p class='severity critical'>Impact: CRITICAL - Immediate patching required!</p>");
                out.println("</div>");
            } else {
                out.println("<div class='attack-result secure'>");
                out.println("<h3>✅ ATTACK BLOCKED</h3>");
                out.println("<p>The Log4Shell payload was safely handled by the patched Log4j " + log4jVersion + " library.</p>");
                out.println("<p><strong>Attack Vector:</strong> <code>" + patientId + "</code></p>");
                out.println("<ul>");
                out.println("<li>✅ JNDI lookup disabled by default</li>");
                out.println("<li>✅ Remote code execution prevented</li>");
                out.println("<li>✅ System remains secure</li>");
                out.println("<li>✅ Patient data protected</li>");
                out.println("</ul>");
                out.println("<p class='severity fixed'>Status: SECURE - Vulnerability patched successfully!</p>");
                out.println("</div>");
            }
            out.println("</div>");
        }
        
        out.println("<a href='index.html' class='back-link'>← Back to Home</a>");
        out.println("</div>");
        out.println("</body></html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
