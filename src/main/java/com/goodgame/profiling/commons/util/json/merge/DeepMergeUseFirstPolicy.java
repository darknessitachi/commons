package com.goodgame.profiling.commons.util.json.merge;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.ext.XLogger;

import com.goodgame.profiling.commons.logging.LogService;

/**
 * Make a deep merge, i.e. recursively merge child objects as well. If two
 * primitive or mismatching values with the same key exist, the first one is
 * used.
 */
public class DeepMergeUseFirstPolicy implements JSONMergePolicy {

    private static final XLogger log = LogService.getXLogger(DeepMergeUseFirstPolicy.class);

    @Override
    public JSONObject merge( JSONObject first, JSONObject second ) {
        log.entry( first, second );

        JSONObject target = new JSONObject();
        @SuppressWarnings( "rawtypes" )
        Iterator keys;
        String key;

        keys = first.keys();
        while ( keys.hasNext() ) {
            key = (String) keys.next();
            target.put( key, first.get( key ) );
        }

        keys = second.keys();
        while ( keys.hasNext() ) {
            key = (String) keys.next();
            if ( !target.has( key ) ) {
                target.put( key, second.get( key ) );

            } else if ( first.get( key ) instanceof JSONArray && second.get( key ) instanceof JSONArray ) {
                JSONArray array1 = first.getJSONArray( key );
                JSONArray array2 = second.getJSONArray( key );
                for ( int i = 0; i < array2.length(); i++ ) {
                    array1.put( array2.get( i ) );
                }
                target.put( key, array1 );

            } else if ( first.get( key ) instanceof JSONObject && second.get( key ) instanceof JSONObject ) {
                JSONObject object1 = first.getJSONObject( key );
                JSONObject object2 = second.getJSONObject( key );
                JSONObject merged = merge( object1, object2 );
                target.put( key, merged );
            }
        }
        log.exit( target );
        return target;
    }

}