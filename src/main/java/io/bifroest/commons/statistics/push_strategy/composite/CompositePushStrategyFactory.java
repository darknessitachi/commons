package io.bifroest.commons.statistics.push_strategy.composite;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.MetaInfServices;

import io.bifroest.commons.boot.interfaces.Environment;
import io.bifroest.commons.statistics.push_strategy.StatisticsPushStrategy;
import io.bifroest.commons.statistics.push_strategy.StatisticsPushStrategyCreator;
import io.bifroest.commons.statistics.push_strategy.StatisticsPushStrategyFactory;

@MetaInfServices
public class CompositePushStrategyFactory<E extends Environment> implements StatisticsPushStrategyFactory<E> {
    @Override
    public String handledType() {
        return "composite";
    }

    @Override
    public StatisticsPushStrategy<E> create( JSONObject config ) {
        JSONArray innerConfigs = config.getJSONArray( "inners" );
        List<StatisticsPushStrategy<E>> inners = new ArrayList<>();

        for( int i = 0; i < innerConfigs.length(); i++ ) {
            inners.add( new StatisticsPushStrategyCreator<E>().create( innerConfigs.getJSONObject( i ) ) );
        }

        return new CompositePushStrategy<E>( inners );
    }
}
