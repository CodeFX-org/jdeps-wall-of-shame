package org.codefx.jwos.analysis.channel;

import com.google.common.collect.ImmutableList;
import org.junit.gen5.api.BeforeEach;
import org.junit.gen5.api.DisplayName;
import org.junit.gen5.api.Nested;
import org.junit.gen5.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("A replaying channel")
class ReplayingTaskChannelDecoratorTest {

	private TaskChannel<String, Integer, Exception> decoratedChannel;
	private TaskChannel<String, Integer, Exception> replayingChannel;

	@BeforeEach
	void mockDecoratedChannel() {
		decoratedChannel = mock(TaskChannel.class);
	}

	@Nested
	@DisplayName("when created")
	class WhenCreatedIncorrectly {

		@Test
		@DisplayName("with null channel, throws NPE")
		void decoratedChannelNull_throwsException() {
			assertThatThrownBy(
					() -> new ReplayingTaskChannelDecorator<>(null, emptySet(), emptySet(), emptySet()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("decoratedChannel");
		}

		@Test
		@DisplayName("with null task replay collection, throws NPE")
		void tasksToReplayNull_throwsException() {
			assertThatThrownBy(
					() -> new ReplayingTaskChannelDecorator<>(decoratedChannel, null, emptySet(), emptySet()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("tasksToReplay");
		}

		@Test
		@DisplayName("with null result replay collection, throws NPE")
		void resultsToReplayNull_throwsException() {
			assertThatThrownBy(
					() -> new ReplayingTaskChannelDecorator<>(decoratedChannel, emptySet(), null, emptySet()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("resultsToReplay");
		}

		@Test
		@DisplayName("with null error replay collection, throws NPE")
		void errorsToReplayNull_throwsException() {
			assertThatThrownBy(
					() -> new ReplayingTaskChannelDecorator<>(decoratedChannel, emptySet(), emptySet(), null))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("errorsToReplay");
		}

	}

	@Nested
	@DisplayName("when created empty")
	class WhenCreatedEmpty {

		@BeforeEach
		void init() {
			replayingChannel = new ReplayingTaskChannelDecorator<>(
					decoratedChannel,
					emptySet(),
					emptySet(),
					emptySet());
		}

		@Test
		@DisplayName("reports decorated channel's size")
		void nrOfWaitingTasks_decoratedChannelSizeGetsReported() {
			when(decoratedChannel.nrOfWaitingTasks()).thenReturn(42);
			assertThat(replayingChannel.nrOfWaitingTasks()).isEqualTo(42);
		}

		@Test
		@DisplayName("reports decorated channel's \"empty state\"")
		void noTasksWaiting_decoratedChannelStateGetsReported() {
			when(decoratedChannel.noWaitingTasks()).thenReturn(false);
			assertThat(replayingChannel.noWaitingTasks()).isEqualTo(false);
		}

		@Test
		@DisplayName("sends task to decorated channel")
		void sendTask_decoratedChannelGetsCalled() {
			replayingChannel.sendTask("Task");
			verify(decoratedChannel).sendTask("Task");
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets task from decorated channel")
		void getTask_decoratedChannelGetsCalled() throws InterruptedException {
			replayingChannel.getTask();
			verify(decoratedChannel).getTask();
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("sends result to decorated channel")
		void sendResult_decoratedChannelGetsCalled() throws InterruptedException {
			replayingChannel.sendResult(5);
			verify(decoratedChannel).sendResult(5);
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("returns only results from decorated channel")
		void drainResults_onlyFromDecoratedChannel() {
			List<Integer> decoratedResults = asList(1, 2, 3);

			when(decoratedChannel.drainResults()).thenReturn(decoratedResults.stream());
			assertThat(replayingChannel.drainResults()).containsExactlyElementsOf(decoratedResults);
		}

		@Test
		@DisplayName("sends errors to decorated channel")
		void sendError_decoratedChannelGetsCalled() throws InterruptedException {
			Exception error = new Exception();

			replayingChannel.sendError(error);

			verify(decoratedChannel).sendError(error);
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("returns only errors from decorated channel")
		void drainErrors_onlyFromDecoratedChannel() {
			List<Exception> decoratedErrors = asList(
					new Exception("X"),
					new Exception("Y")
			);

			Stream<Exception> errors = decoratedChannel.drainErrors();

			when(errors).thenReturn(decoratedErrors.stream());
			assertThat(replayingChannel.drainErrors()).containsExactlyElementsOf(decoratedErrors);
		}

	}


	@Nested
	@DisplayName("when created with elements")
	class WhenCreatedWithElements {

		private final List<String> tasksToReplay = of("1", "2", "C");
		private final List<Integer> resultsToReplay = of(1, 2);
		private final List<Exception> errorsToReplay = of(new Exception("C"));

		@BeforeEach
		void createReplayingChannel() {
			replayingChannel = new ReplayingTaskChannelDecorator<>(
					decoratedChannel,
					tasksToReplay,
					resultsToReplay,
					errorsToReplay);
		}

		@Test
		@DisplayName("reports own's plus decorated channel's size")
		void nrOfWaitingTasks_decoratedChannelSizeGetsReported() {
			when(decoratedChannel.nrOfWaitingTasks()).thenReturn(0);
			assertThat(replayingChannel.nrOfWaitingTasks()).isEqualTo(3);
		}

		@Test
		@DisplayName("reports own's plus decorated channel's \"empty state\"")
		void noTasksWaiting_decoratedChannelStateGetsReported() {
			when(decoratedChannel.noWaitingTasks()).thenReturn(true);
			assertThat(replayingChannel.noWaitingTasks()).isEqualTo(false);
		}

		@Test
		@DisplayName("sends task to decorated channel")
		void sendTask_decoratedChannelGetsCalled() {
			replayingChannel.sendTask("Task");
			verify(decoratedChannel).sendTask("Task");
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets tasks from replay, then from decorated channel")
		void getTask_tasksGetReplayed() throws InterruptedException {
			List<String> decoratedTasks = of("X", "Y", "Z");
			OngoingStubbing<String> stubbingGetTask = when(decoratedChannel.getTask());
			for (String task : decoratedTasks)
				stubbingGetTask = stubbingGetTask.thenReturn(task);
			int taskTotal = tasksToReplay.size() + decoratedTasks.size();

			List<String> tasks = takeTimes(replayingChannel::getTask, taskTotal);

			assertThat(tasks).containsExactlyElementsOf(concat(tasksToReplay, decoratedTasks));
			verify(decoratedChannel, times(decoratedTasks.size())).getTask();
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("sends result to decorated channel")
		void sendResult_decoratedChannelGetsCalled() throws InterruptedException {
			replayingChannel.sendResult(5);
			verify(decoratedChannel).sendResult(5);
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets results from replay, then from decorated channel")
		void drainResults_resultsGetReplayed() {
			List<Integer> decoratedResults = asList(10, 20, 30);
			when(decoratedChannel.drainResults()).thenReturn(decoratedResults.stream());

			Stream<Integer> results = replayingChannel.drainResults();

			assertThat(results).containsExactlyElementsOf(concat(resultsToReplay, decoratedResults));
			verify(decoratedChannel).drainResults();
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets results from replay only once")
		void drainResults_resultsGetReplayedOnce() {
			List<Integer> decoratedResults_1 = asList(10, 20, 30);
			List<Integer> decoratedResults_2 = asList(100, 200, 300);
			when(decoratedChannel.drainResults()).thenReturn(decoratedResults_1.stream(), decoratedResults_2.stream());

			// the first call should drain the replay and call the stubbed method for the first time
			replayingChannel.drainResults().forEach(ignored -> {
			});
			// the second call should return only the result from the stubbed method
			Stream<Integer> results = replayingChannel.drainResults();

			assertThat(results).containsExactlyElementsOf(decoratedResults_2);
		}

		@Test
		@DisplayName("sends errors to decorated channel")
		void sendError_decoratedChannelGetsCalled() throws InterruptedException {
			Exception error = new Exception();
			replayingChannel.sendError(error);

			verify(decoratedChannel).sendError(error);
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets errors from replay, then from decorated channel")
		void drainErrors_errorsGetReplayed() {
			List<Exception> decoratedErrors = asList(new Exception("ABC"), new Exception("XYZ"));
			when(decoratedChannel.drainErrors()).thenReturn(decoratedErrors.stream());

			Stream<Exception> errors = replayingChannel.drainErrors();

			assertThat(errors).containsExactlyElementsOf(concat(errorsToReplay, decoratedErrors));
			verify(decoratedChannel).drainErrors();
			verifyNoMoreInteractions(decoratedChannel);
		}

		@Test
		@DisplayName("gets errors from replay only once")
		void drainErrors_resultsGetReplayedOnce() {
			List<Exception> decoratedErrors_1 = asList(new Exception("A"), new Exception("X"));
			List<Exception> decoratedErrors_2 = asList(new Exception("ABC"), new Exception("XYZ"));
			when(decoratedChannel.drainErrors()).thenReturn(decoratedErrors_1.stream(), decoratedErrors_2.stream());

			// the first call should drain the replay and call the stubbed method for the first time
			replayingChannel.drainErrors().forEach(ignored -> {
			});
			// the second call should return only the result from the stubbed method
			Stream<Exception> results = replayingChannel.drainErrors();

			assertThat(results).containsExactlyElementsOf(decoratedErrors_2);
		}

		@Nested
		@DisplayName("when created with elements and then drained")
		class WhenCreatedWithElementsThenDrained {

			private final List<String> tasksToReplay = of("1", "2", "C");
			private final List<Integer> resultsToReplay = of(1, 2);
			private final List<Exception> errorsToReplay = of(new Exception("C"));

			@BeforeEach
			void createReplayingChannelThenDrain() {
				replayingChannel = new ReplayingTaskChannelDecorator<>(
						decoratedChannel,
						tasksToReplay,
						resultsToReplay,
						errorsToReplay);
				when(decoratedChannel.drainTasks()).thenReturn(Stream.of());
				when(decoratedChannel.drainResults()).thenReturn(Stream.of());
				when(decoratedChannel.drainErrors()).thenReturn(Stream.of());
				replayingChannel.drainTasks().forEach(ignore -> { });
				replayingChannel.drainResults().forEach(ignore -> { });
				replayingChannel.drainErrors().forEach(ignore -> { });
			}

			@Test
			@DisplayName("reports own's plus decorated channel's size")
			void nrOfWaitingTasks_computedChannelSizeGetsReported() {
				when(decoratedChannel.nrOfWaitingTasks()).thenReturn(0);
				assertThat(replayingChannel.nrOfWaitingTasks()).isEqualTo(0);
			}

			@Test
			@DisplayName("reports own's plus decorated channel's \"empty state\"")
			void noTasksWaiting_computedChannelStateGetsReported() {
				when(decoratedChannel.noWaitingTasks()).thenReturn(true);
				assertThat(replayingChannel.noWaitingTasks()).isEqualTo(true);
			}

		}

	}

	// UTILITIES

	private static <E> ImmutableList<E> takeTimes(Take<E> take, int times) throws InterruptedException {
		ImmutableList.Builder<E> elements = ImmutableList.builder();
		for (int i = 0; i < times; i++)
			elements.add(take.take());
		return elements.build();
	}

	private interface Take<T> {
		T take() throws InterruptedException;
	}

}
