/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: DependencyResolver.java,v 1.3 2005-06-18 04:58:13 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.util;

import henplus.sqlmodel.ColumnFkInfo;
import henplus.sqlmodel.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves dependencies between a given set of tables in respect to their foreign keys.<br>
 * Created on: Sep 20, 2004<br>
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 * @version $Id: DependencyResolver.java,v 1.3 2005-06-18 04:58:13 hzeller Exp $
 */
public final class DependencyResolver {
    
    private final Iterator _tableIter;
    private Set cyclicDependencies/*<List<Table>>*/;
    
    /**
     * @param tableIter An <code>Iterator</code> over <code>Table</code>s.
     */
    public DependencyResolver( Iterator/*<Table>*/ tableIter ) {
        _tableIter = tableIter;
    };
    
    /**
     * @param tables A <code>Set</code> of <code>Table</code> objects.
     */
    public DependencyResolver( Set/*<Table>*/ tables ) {
        _tableIter = ( tables != null ) ? tables.iterator() : null;
    }
    
    /**
     * @return
     * 
     */
    public ResolverResult sortTables() {
        ListMap resolved = new ListMap();
        Map unresolved = null;
        
        // first run: separate tables with and without dependencies
        while( _tableIter.hasNext() ) {
            Table t = (Table) _tableIter.next();
            if (t == null) {
                continue;
            }
            Set fks = t.getForeignKeys();
            
            // no dependency / foreign key?
            if ( fks == null ) {
                // System.out.println( "[sortTables] put " + t + " to resolved." );
                resolved.put( t.getName(), t );
            }
            else {
                // dependency fulfilled?
                boolean nodep = true;
                Iterator iter2 = fks.iterator();
                while ( iter2.hasNext() && nodep ) {
                    ColumnFkInfo fk = (ColumnFkInfo) iter2.next();
                    if ( !resolved.containsKey( fk.getPkTable() ) ) {
                        nodep = false;
                    }
                }
                
                if ( nodep ) {
                    // System.out.println( "[sortTables] put " + t + " to resolved." );
                    resolved.put( t.getName(), t );
                }
                else {
                    if ( unresolved == null )
                        unresolved = new HashMap();
                    // System.out.println( "[sortTables] put " + t + " to unresolved." );
                    unresolved.put( t.getName(), t );
                }
            }
        }
        
        // second run: we check remaining deps
        if ( unresolved != null ) {
            Iterator iter = unresolved.values().iterator();
            while ( iter.hasNext() ) {
                Table t = (Table) iter.next();
                resolveDep( t, null, resolved, unresolved );
            }
        }
        
        // do we need a second run?
        // unresolved = cleanUnresolved( resolved, unresolved );
        
        // add all unresolved/conflicting tables to the resulting list
        List result = resolved.valuesList();
        if ( unresolved != null ) {
            Iterator iter = unresolved.values().iterator();
            while ( iter.hasNext() ) {
                Object table = iter.next();
                if ( !result.contains(table) )
                    result.add( table );
            }
        }
        
        return new ResolverResult( result, cyclicDependencies );
    }

    /**
     * @return
     * 
     */
    /* Martin: needed ?
    private Set restructureDeps() {
        Set deps = null;
        if ( cyclicDependencies != null ) {
            deps = new HashSet();
            Iterator iter = cyclicDependencies.iterator();
            while ( iter.hasNext() )
                deps.add( ((ListMap)iter.next()).valuesList() );
        }
        return deps;
    }
*/
    /**
     * @param resolved
     * @param unresolved
     * @return A Map which contains all yet unresolved Tables mapped to their names.
     */
    /* Martin: needed ?
    private Map cleanUnresolved( Map resolved, Map unresolved ) {
        Map result = null;
        
        if ( unresolved != null ) {
            Iterator iter = unresolved.keySet().iterator();
            while ( iter.hasNext() ) {
                // type element = (type) iter.next();
                
            }
        }
        
        return null;
    }
*/
    /**
     * @param t
     * @param cyclePath	The path of tables which have cyclic dependencies
     * @param resolved
     * @param unresolved
     */
    private void resolveDep( Table t, List/*<Table>*/ cyclePath, Map resolved, Map unresolved ) {

        // System.out.println( "[resolveDep] >>> Starting for t: " + t + " and cyclePath: " + cyclePath );
        
        // if the current table is no more in the unresolved collection
        if ( t == null || resolved.containsKey( t.getName() ) )
            return;
        
        boolean nodep = false;
        boolean firstrun = true;
        Set fks = t.getForeignKeys();
        Iterator iter = fks.iterator();
        while ( iter.hasNext() ) {
            ColumnFkInfo fk = (ColumnFkInfo) iter.next();

            // System.out.println( "[resolveDep] FK -> " + fk.getPkTable() + ": " + resolved.containsKey( fk.getPkTable() ) );
            if ( !resolved.containsKey( fk.getPkTable() ) ) {
                
                Table inner = (Table) unresolved.get( fk.getPkTable() );
                
                // if there's yet a cycle with the two tables inner following t
                // then proceed to the next FK and ignore this potential cycle
                if ( duplicateCycle( t, inner ) )
                    continue;
                
                if ( cyclePath != null && cyclePath.contains( inner ) ) {
                    
                        cyclePath.add( t );
                        
                        // create a new list for the detected cycle to add to the
                        // cyclicDeps, the former one (cyclePath) is used further on
                        List cycle = new ArrayList( cyclePath );
                        cycle.add( inner );
                        if ( cyclicDependencies == null )
                            cyclicDependencies = new HashSet();
                        // System.out.println("[resolveDep] +++ Putting cyclePath: " + cycle );
                        cyclicDependencies.add( cycle );
                        continue;
                        
                }
                else {
                    if ( cyclePath == null ) {
                        // System.out.println("[resolveDep] Starting cyclePath with: " + t);
                        cyclePath = new ArrayList();
                    }
                    cyclePath.add( t );
                }
                
                resolveDep( inner, cyclePath, resolved, unresolved );
                
                if ( resolved.containsKey( fk.getPkTable() ) ) {
                    nodep = (firstrun || nodep) && true;
                    firstrun = false;
                }
            }
            else {
                nodep = (firstrun || nodep) && true;
                firstrun = false;
            }
        }
        
        if ( nodep && !resolved.containsKey( t.getName() ) ) {
            // System.out.println( "[resolveDep] put " + t + " to resolved." );
            resolved.put( t.getName(), t );
        }
        
    }
    
    /**
     * Tests if there's yet a cycle (stored in cyclicDependencies) with
     * the given tables t and inner, whith inner following t.
     * @param t
     * @param inner
     * @return
     */
    private boolean duplicateCycle( Table t, Table inner ) {
        boolean result = false;
        if ( cyclicDependencies != null ) {
            Iterator iter = cyclicDependencies.iterator();
            while ( iter.hasNext() && !result ) {
                List path = (List)iter.next();
                if ( path.contains( t ) ) {
                    int tIdx = path.indexOf( t );
                    if ( path.size() > tIdx + 1 && inner.equals( path.get( tIdx + 1 ) ) ) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
    
    public class ResolverResult {
        private final List/*<Table>*/ _tables;
        private final Set/*<List<Table>>*/ _cyclicDependencies;
        public ResolverResult( List tables, Set cyclicDependencies ) {
            _tables = tables;
            _cyclicDependencies = cyclicDependencies;
        }
        /**
         * @return Returns the cyclicDependencies: a <code>Set</code> holding
         * <code>List</code>s of <code>CycleEntry</code> objects, where each list
         * represents the path of a cyclic dependency.
         */
        public Set/*<List<Table>>*/ getCyclicDependencies() {
            return _cyclicDependencies;
        }
        /**
         * @return Returns the tables.
         */
        public List/*<Table>*/ getTables() {
            return _tables;
        }
    }
    
    public class CycleEntry {
        private Table _table;
        private ColumnFkInfo _fk;
        public CycleEntry( Table table, ColumnFkInfo fk ) {
            _table = table;
            _fk = fk;
        }
        /**
         * @return Returns the fk.
         */
        public ColumnFkInfo getFk() {
            return _fk;
        }
        /**
         * @return Returns the table.
         */
        public Table getTable() {
            return _table;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer( "CycleEntry [" );
            sb.append( "table: " ).append( _table.getName() );
            sb.append( ", fk: " ).append( _fk.toString() );
            sb.append( "]" );
            return sb.toString();
        }
        public boolean equals( Object other ) {
            boolean result = false;
            if ( other == this )
                result = true;
            else if ( other instanceof CycleEntry ) {
                CycleEntry ce = (CycleEntry)other;
                if ( _table == null && ce.getTable() == null
                        && _fk == null && ce.getFk() == null )
                    result = true;
                else if ( _table.equals( ce.getTable() )
                        && _fk.equals( ce.getFk() ) )
                    result = true;
            }
            return result;
        }
        @Override
        public int hashCode() {
            return ObjectUtil.nullSafeHashCode(_table) 
            ^ ObjectUtil.nullSafeHashCode(_fk);
        }
    }

}
