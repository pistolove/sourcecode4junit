/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.authenticator;


import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;



/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation that checks
 * only security constraints not involving user authentication.
 *
 * @author Craig R. McClanahan
 * @version $Id: NonLoginAuthenticator.java 1225469 2011-12-29 08:09:40Z markt $
 */

public final class NonLoginAuthenticator
    extends AuthenticatorBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * Descriptive information about this implementation.
     */
    private static final String info =
        "org.apache.catalina.authenticator.NonLoginAuthenticator/1.0";


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {

        return (info);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * <p>Authenticate the user making this request, based on the fact that no
     * <code>login-config</code> has been defined for the container.</p>
     *
     * <p>This implementation means "login the user even though there is no
     * self-contained way to establish a security Principal for that user".</p>
     *
     * <p>This method is called by the AuthenticatorBase super class to
     * establish a Principal for the user BEFORE the container security
     * constraints are examined, i.e. it is not yet known whether the user
     * will eventually be permitted to access the requested resource.
     * Therefore, it is necessary to always return <code>true</code> to
     * indicate the user has not failed authentication.</p>
     *
     * <p>There are two cases:
     * <ul>
     * <li>without SingleSignon: a Session instance does not yet exist
     *     and there is no <code>auth-method</code> to authenticate the
     *     user, so leave Request's Principal as null.
     *     Note: AuthenticatorBase will later examine the security constraints
     *           to determine whether the resource is accessible by a user
     *           without a security Principal and Role (i.e. unauthenticated).
     * </li>
     * <li>with SingleSignon: if the user has already authenticated via
     *     another container (using its own login configuration), then
     *     associate this Session with the SSOEntry so it inherits the
     *     already-established security Principal and associated Roles.
     *     Note: This particular session will become a full member of the
     *           SingleSignOnEntry Session collection and so will potentially
     *           keep the SSOE "alive", even if all the other properly
     *           authenticated Sessions expire first... until it expires too.
     * </li>
     * </ul></p>
     *
     * @param request  Request we are processing
     * @param response Response we are creating
     * @param config   Login configuration describing how authentication
     *                 should be performed
     * @return boolean to indicate whether the user is authenticated
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean authenticate(Request request,
                                HttpServletResponse response,
                                LoginConfig config)
        throws IOException {

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            // excellent... we have already authenticated the client somehow,
            // probably from another container that has a login-config
            if (containerLog.isDebugEnabled())
                containerLog.debug("Already authenticated as '"
                          + principal.getName() + "'");

            if (cache) {
                // create a new session (only if necessary)
                Session session = request.getSessionInternal(true);

                // save the inherited Principal (if necessary) in this
                // session so it can remain authenticated until it expires
                session.setPrincipal(principal);

                // is there an SSO session cookie?
                String ssoId =
                        (String) request.getNote(Constants.REQ_SSOID_NOTE);
                if (ssoId != null) {
                    if (containerLog.isDebugEnabled())
                        containerLog.debug("User authenticated by existing SSO");
                    // Associate session with the existing SSO ID if necessary
                    associate(ssoId, session);
                }
            }

            // user was already authenticated, with or without a cookie
            return true;
        }

        // No Principal means the user is not already authenticated
        // and so will not be assigned any roles. It is safe to
        // to say the user is now authenticated because access to
        // protected resources will only be allowed with a matching role.
        // i.e. SC_FORBIDDEN (403 status) will be generated later.

        if (containerLog.isDebugEnabled())
            containerLog.debug("User authenticated without any roles");
        return true;
    }


    /**
     * Return the authentication method, which is vendor-specific and
     * not defined by HttpServletRequest.
     */
    @Override
    protected String getAuthMethod() {
        return "NONE";
    }
}
