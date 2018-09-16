/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.custom.data.publisher.local;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.data.publisher.application.authentication.AbstractAuthenticationDataPublisher;
import org.wso2.carbon.identity.data.publisher.application.authentication.AuthPublisherConstants;
import org.wso2.carbon.identity.data.publisher.application.authentication.model.AuthenticationData;
import org.wso2.carbon.identity.data.publisher.application.authentication.model.SessionData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class CustomSessionDataPublisherImpl extends AbstractAuthenticationDataPublisher {

    public static final Log log = LogFactory.getLog(CustomSessionDataPublisherImpl.class);
    private static final String SESSION_DATA_PERSIST_QUERY = "INSERT INTO IDN_CUSTOM_SESSION_DATA (`USER`, " +
            "`SESSION_ID`, `TIMESTAMP`, `ACTION`, `SERVICE_PROVIDER`) VALUES (?, ?, ?, ?, ?)";
    private static final String SESSION_DATA_RETRIVE_QUERY = "SELECT `ID`, `SESSION_ID` FROM " +
            "IDN_CUSTOM_SESSION_DATA WHERE `USER`=? AND `SERVICE_PROVIDER`=? ORDER BY `TIMESTAMP` LIMIT 1";
    private static final String SESSION_DATA_UPDATE_QUERY = "UPDATE IDN_CUSTOM_SESSION_DATA SET ACTION = ?, " +
            "TIMESTAMP = ? WHERE ID = ?";
    private static final String SESSION_DATA_DELETE_QUERY = "DELETE FROM IDN_CUSTOM_SESSION_DATA WHERE ID = ?";

    @Override
    public void publishAuthenticationStepSuccess(HttpServletRequest request, AuthenticationContext context,
                                                 Map<String, Object> params) {
        // This method is overridden to do nothing since this is a session data publisher.
    }

    @Override
    public void publishAuthenticationStepFailure(HttpServletRequest request, AuthenticationContext context,
                                                 Map<String, Object> params) {
        // This method is overridden to do nothing since this is a session data publisher.
    }

    @Override
    public void publishAuthenticationSuccess(HttpServletRequest request, AuthenticationContext context, Map<String,
            Object> params) {
        // This method is overridden to do nothing since this is a session data publisher.
    }

    @Override
    public void publishAuthenticationFailure(HttpServletRequest request, AuthenticationContext context, Map<String,
            Object> params) {
        // This method is overridden to do nothing since this is a session data publisher.
    }

    @Override
    public void doPublishAuthenticationStepSuccess(AuthenticationData authenticationData) {
        // This method is not implemented since there is no usage of it in session publishing
    }

    @Override
    public void doPublishAuthenticationStepFailure(AuthenticationData authenticationData) {
        // This method is not implemented since there is no usage of it in session publishing
    }

    @Override
    public void doPublishAuthenticationSuccess(AuthenticationData authenticationData) {
        // This method is not implemented since there is no usage of it in session publishing
    }

    @Override
    public void doPublishAuthenticationFailure(AuthenticationData authenticationData) {
        // This method is not implemented since there is no usage of it in session publishing
    }

    @Override
    public void doPublishSessionCreation(SessionData sessionData) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing session creation to DAS");
        }
        publishSessionData(sessionData, AuthPublisherConstants.SESSION_CREATION_STATUS);
    }

    @Override
    public void doPublishSessionTermination(SessionData sessionData) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing session termination to DAS");
        }
        publishSessionData(sessionData, AuthPublisherConstants.SESSION_TERMINATION_STATUS);
    }

    @Override
    public void doPublishSessionUpdate(SessionData sessionData) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing session update to DAS");
        }
        publishSessionData(sessionData, AuthPublisherConstants.SESSION_UPDATE_STATUS);
    }

    @Override
    public String getName() {
        return AuthPublisherConstants.DAS_SESSION_PUBLISHER_NAME;
    }

    protected void publishSessionData(SessionData sessionData, int actionId) {

        if (sessionData != null) {

            String newAction = "";
            switch (actionId) {
                case 0: newAction = "Terminated"; break;
                case 1: newAction = "Created"; break;
                case 2: newAction = "Updated"; break;
            }

            log.info("Persisting session data record: [Action: " + newAction + ", User: " + sessionData.getUser() +
                    ", Session ID: " + sessionData.getSessionId());

            PreparedStatement prepStmt1 = null;
            PreparedStatement prepStmt2 = null;
            Connection connection = null;

            // Get existing session records.
            try {
                connection = IdentityDatabaseUtil.getDBConnection();
                prepStmt1 = connection.prepareStatement(SESSION_DATA_RETRIVE_QUERY);
                prepStmt1.setString(1, sessionData.getUser());
                prepStmt1.setString(2, sessionData.getServiceProvider());
                ResultSet resultSet = prepStmt1.executeQuery();

                int id = 0;
                String sessionId = null;
                // This result set will have either 0 or only 1 record.
                while (resultSet.next()) {
                    id = resultSet.getInt(1);
                    sessionId = resultSet.getString(2);
                }

                if (sessionId != null) {
                    if (newAction.equals("Updated")) {
                        // updating the existing record.
                        try {
                            connection = IdentityDatabaseUtil.getDBConnection();
                            prepStmt2 = connection.prepareStatement(SESSION_DATA_UPDATE_QUERY);
                            prepStmt2.setString(1, newAction);
                            prepStmt2.setTimestamp(2, new Timestamp(sessionData.getUpdatedTimestamp()));
                            prepStmt2.setInt(3, id);
                            prepStmt2.execute();
                        } catch (SQLException e) {
                            log.error("Error while updating custom user session information.", e);
                        }
                    } else if (newAction.equals("Terminated")) {
                        // Deleting the record.
                        try {
                            connection = IdentityDatabaseUtil.getDBConnection();
                            prepStmt2 = connection.prepareStatement(SESSION_DATA_DELETE_QUERY);
                            prepStmt2.setInt(1, id);
                            prepStmt2.execute();
                        } catch (SQLException e) {
                            log.error("Error while deleting custom user session information.", e);
                        }
                    }
                } else {
                    // Insert new record to the DB.
                    try {
                        connection = IdentityDatabaseUtil.getDBConnection();
                        prepStmt2 = connection.prepareStatement(SESSION_DATA_PERSIST_QUERY);
                        prepStmt2.setString(1, sessionData.getUser());
                        prepStmt2.setString(2, sessionData.getSessionId());
                        prepStmt2.setTimestamp(3, new Timestamp(sessionData.getCreatedTimestamp()));
                        prepStmt2.setString(4, newAction);
                        prepStmt2.setString(5, sessionData.getServiceProvider());
                        prepStmt2.execute();
                    } catch (SQLException e) {
                        log.error("Error while persisting custom user session information.", e);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                log.error("Error while persisting custom user session information.", e);
            } finally {
                IdentityDatabaseUtil.closeStatement(prepStmt1);
                IdentityDatabaseUtil.closeStatement(prepStmt2);
                IdentityDatabaseUtil.closeConnection(connection);
            }
        }
    }
}
