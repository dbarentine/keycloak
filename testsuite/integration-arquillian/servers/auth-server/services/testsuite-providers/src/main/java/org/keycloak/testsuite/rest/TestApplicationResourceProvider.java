/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.rest;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.events.EventsListenerProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class TestApplicationResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;

    private final BlockingQueue<LogoutAction> adminLogoutActions;
    private final BlockingQueue<PushNotBeforeAction> adminPushNotBeforeActions;

    public TestApplicationResourceProvider(KeycloakSession session, BlockingQueue<LogoutAction> adminLogoutActions,
            BlockingQueue<PushNotBeforeAction> adminPushNotBeforeActions) {
        this.session = session;
        this.adminLogoutActions = adminLogoutActions;
        this.adminPushNotBeforeActions = adminPushNotBeforeActions;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/admin/k_logout")
    public void adminLogout(String data) throws JWSInputException {
        adminLogoutActions.add(new JWSInput(data).readJsonContent(LogoutAction.class));
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/admin/k_push_not_before")
    public void adminPushNotBefore(String data) throws JWSInputException {
        adminPushNotBeforeActions.add(new JWSInput(data).readJsonContent(PushNotBeforeAction.class));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-logout")
    public LogoutAction getAdminLogoutAction() throws InterruptedException {
        return adminLogoutActions.poll(10, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-not-before")
    public PushNotBeforeAction getAdminPushNotBefore() throws InterruptedException {
        return adminPushNotBeforeActions.poll(10, TimeUnit.SECONDS);
    }

    @POST
    @Path("/clear-admin-actions")
    public Response clearAdminActions() {
        adminLogoutActions.clear();
        adminPushNotBeforeActions.clear();
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/{action}")
    public String get(@PathParam("action") String action) {
        //String requestUri = session.getContext().getUri().getRequestUri().toString();

        String title = "APP_REQUEST";
         if (action.equals("auth")) {
            title = "AUTH_RESPONSE";
        } else if (action.equals("logout")) {
            title = "LOGOUT_REQUEST";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>" + title + "</title></head><body>");
        UriBuilder base = UriBuilder.fromUri("http://localhost:8180/auth");
        sb.append("<a href=\"" + RealmsResource.accountUrl(base).build("test").toString() + "\" id=\"account\">account</a>");

        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
