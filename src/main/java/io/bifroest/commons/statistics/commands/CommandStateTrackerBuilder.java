package io.bifroest.commons.statistics.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.apache.commons.collections4.map.LazyMap;

import io.bifroest.commons.statistics.EventWithInstant;
import io.bifroest.commons.statistics.WriteToStorageEvent;
import io.bifroest.commons.statistics.eventbus.EventBusManager;
import io.bifroest.commons.statistics.eventbus.EventBusRegistrationPoint;
import io.bifroest.commons.statistics.storage.MetricStorage;
import io.bifroest.commons.util.stopwatch.AsyncClock;
import io.bifroest.commons.util.stopwatch.StopWatchWithStates;

public class CommandStateTrackerBuilder {
    private final AsyncClock clock;

    private final EventBusRegistrationPoint registrationPoint = EventBusManager.createRegistrationPoint();

    // if an unordered set of witches watch a map of watches
    // in entry iteration order, which witch would watch which
    // subset of watches?
    private final Map<Long, StopWatchWithStates> watches;

    private CommandStateTrackerBuilder( String command, String[] substorageNames ) {
        clock = new AsyncClock();

        watches = LazyMap.lazyMap( new HashMap<Long, StopWatchWithStates>(), 
                                       () -> new StopWatchWithStates( clock ) );

        registrationPoint.sub( WriteToStorageEvent.class, e -> {
            clock.setInstant( e.when() );
            MetricStorage destination = e.storageToWriteTo();

            for ( String subStorageName : substorageNames ) {
                destination = destination.getSubStorageCalled( subStorageName );
            }

            MetricStorage finalDestination = destination.getSubStorageCalled( command );
            Map<String, LongAdder> stageRuntimes = LazyMap.lazyMap( new HashMap<>(), () -> new LongAdder() );
            watches.forEach( (id, watch) -> {
                watch.consumeStateDurations( (stage, duration) -> {
                    stageRuntimes.get( stage ).add( duration.toNanos() );
                });
            });

            stageRuntimes.forEach( (stage, time) -> {
                finalDestination.store( stage, time.doubleValue() );
            });
        });
    }

    public static CommandStateTrackerBuilder listeningForCommandAndStoringIn( String command, String... substorages ) {
        return new CommandStateTrackerBuilder( command, substorages );
    }

    public <E extends EventWithThreadId&EventWithInstant> CommandStateTrackerBuilder withState( Class<E> eventClass, String name ) {
        return withState( eventClass, name, e -> true );
    }
    
    public <E extends EventWithThreadId&EventWithInstant> CommandStateTrackerBuilder withState( Class<E> eventClass, String name, Predicate<E> pred ) {
        registrationPoint.sub( eventClass, e -> {
            if( !pred.test( e ) ) return;
            clock.setInstant( e.when() );
            watches.get( e.threadId() ).startState( name );
        });
        return this;
    }
    
    public <E extends EventWithThreadId&EventWithInstant> CommandStateTrackerBuilder withStoppingEvent( Class<E> eventClass ) {
        return withStoppingEvent( eventClass );
    }
    
    public <E extends EventWithThreadId&EventWithInstant> CommandStateTrackerBuilder withStoppingEvent( Class<E> eventClass, Predicate<E> pred ) {
        registrationPoint.sub( eventClass, e -> {
            if( !pred.test( e ) ) return;
            clock.setInstant( e.when() );
            watches.get( e.threadId() ).stop();
        });
        return this;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals( Object o ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Why do you print this" + getClass() + "?!";
    }
}
