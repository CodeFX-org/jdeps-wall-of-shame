package org.codefx.jwos.analysis.channel;

import org.junit.gen5.api.BeforeEach;
import org.junit.gen5.api.DisplayName;
import org.junit.gen5.api.Nested;
import org.junit.gen5.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("A spying channel")
class SpyingTaskChannelDecoratorTest {

	private TaskChannel<String, Integer, Exception> decoratedChannel;
	private TaskChannel<String, Integer, Exception> listeningChannel;
	private TaskChannel<String, Integer, Exception> spyingChannel;

	@BeforeEach
	void mockDecoratedAndListeningChannel() {
		decoratedChannel = mock(TaskChannel.class);
		listeningChannel = mock(TaskChannel.class);
	}

	@Nested
	@DisplayName("when created")
	class WhenCreatedIncorrectly {

		@Test
		@DisplayName("with null decorated channel, throws NPE")
		void decoratedChannelNull_throwsException() {
			assertThatThrownBy(
					() -> new SpyingTaskChannelDecorator<>(null, listeningChannel))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("decoratedChannel");
		}

		@Test
		@DisplayName("with null listening channel, throws NPE")
		void listeningChannelNull_throwsException() {
			assertThatThrownBy(
					() -> new SpyingTaskChannelDecorator<>(decoratedChannel, null))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("listeningChannel");
		}

	}

	@Nested
	@DisplayName("will relay")
	class WillRelay {

		@BeforeEach
		void createSpyingChannel() {
			spyingChannel = new SpyingTaskChannelDecorator<>(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("tasks")
		void sendTask_taskGetsRelayed() {
			spyingChannel.sendTask("X");

			verify(decoratedChannel).sendTask("X");
			verify(listeningChannel).sendTask("X");
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("results")
		void sendResult_resultGetsRelayed() throws InterruptedException {
			spyingChannel.sendResult(1);

			verify(decoratedChannel).sendResult(1);
			verify(listeningChannel).sendResult(1);
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("errors")
		void sendError_errorGetsRelayed() throws InterruptedException {
			Exception error = new Exception("ABC");
			spyingChannel.sendError(error);

			verify(decoratedChannel).sendError(error);
			verify(listeningChannel).sendError(error);
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

	}

	@Nested
	@DisplayName("will delegate queries for")
	class WillDelegateQueries {

		@BeforeEach
		void createSpyingChannel() {
			spyingChannel = new SpyingTaskChannelDecorator<>(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("tasks")
		void getTask_callIsDelegated() throws InterruptedException {
			spyingChannel.getTask();

			verify(decoratedChannel).getTask();
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("results")
		void sendResult_resultGetsRelayed() throws InterruptedException {
			spyingChannel.drainResults();

			verify(decoratedChannel).drainResults();
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

		@Test
		@DisplayName("errors")
		void sendError_errorGetsRelayed() throws InterruptedException {
			spyingChannel.drainErrors();

			verify(decoratedChannel).drainErrors();
			verifyNoMoreInteractions(decoratedChannel, listeningChannel);
		}

	}

}
