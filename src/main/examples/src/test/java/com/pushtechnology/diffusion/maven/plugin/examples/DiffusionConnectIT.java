package com.pushtechnology.diffusion.maven.plugin.examples;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.Session.State;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;



/**
 * An example of a Diffusion integration test using the diffusion-maven-plugin
 */
public class DiffusionConnectIT {
    @Mock
    private Session.Listener sessionListener;

    @Mock
    private ValueStream<String> topicStream;

    @Mock
    private Topics.CompletionCallback callback;

    @Before
    public void setUp() {
        openMocks(this);
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
        final TopicControl topicControl = session.feature(TopicControl.class);
        final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);
        final CompletableFuture<TopicControl.AddTopicResult> future = topicControl.addTopic(
            "foo", TopicType.STRING);

        // Wait for the CompletableFuture to complete
        future.get(10, TimeUnit.SECONDS);

        topics.addStream("foo", String.class, topicStream);
        topics.subscribe("foo", callback);
        topicUpdate.set("foo", String.class, "bar");
        topicUpdate.set("foo", String.class, "bars");

        verify(callback, timeout(10000)).onComplete();

        verify(topicStream, timeout(10000)).onSubscription(
            isNotNull(String.class),
            isNotNull(TopicSpecification.class));
        verify(topicStream, timeout(10000)).onValue(
            isNotNull(String.class),
            isNotNull(TopicSpecification.class),
            isNotNull(String.class),
            isNotNull(String.class));
    }
}
