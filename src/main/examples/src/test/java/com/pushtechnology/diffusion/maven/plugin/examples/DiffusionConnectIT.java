package com.pushtechnology.diffusion.maven.plugin.examples;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.Session.State;
import com.pushtechnology.diffusion.client.topics.details.TopicDetails;
import com.pushtechnology.diffusion.client.types.UpdateContext;

/**
 * An example of a Diffusion integration test using the diffusion-maven-plugin
 */
public class DiffusionConnectIT {
    @Mock
    private Session.Listener sessionListener;

    @Mock
    private TopicStream topicStream;

    @Mock
    private Topics.CompletionCallback callback;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @After
    public void postConditions() {
        verifyNoMoreInteractions(sessionListener);
    }

    public DiffusionConnectIT() {
    }

    @Test
    public void testSession() throws Exception {

        final Session session =
                Diffusion.sessions().principal("admin").password("password")
                        .connectionTimeout(10000)
                        .listener(sessionListener)
                        .errorHandler(new Session.ErrorHandler.Default())
                        .open("ws://localhost:8080");

        verify(sessionListener).onSessionStateChanged(
                session,
                State.CONNECTING,
                State.CONNECTED_ACTIVE);

        assertNotNull(session.getSessionId());

        final Topics topics = session.feature(Topics.class);

        topics.addFallbackTopicStream(topicStream);
        topics.subscribe(">Diffusion", callback);

        verify(callback, timeout(10000)).onComplete();

        verify(topicStream, timeout(10000)).onSubscription(
                isA(String.class),
                any(TopicDetails.class));
        verify(topicStream, timeout(10000)).onTopicUpdate(
                isA(String.class),
                isA(Content.class),
                isA(UpdateContext.class));

    }
}
