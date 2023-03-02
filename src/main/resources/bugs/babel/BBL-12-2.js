[ , ... function * ( ) { yield * { [ Symbol . iterator ] : async x => 0 } ; } ( ) ] ; 
for ( var x of function * ( ) { yield * { [ Symbol . iterator ] : x => 0 } ; } ( ) ) ; 
for ( var x of function * x ( ) { yield * { [ Symbol . iterator ] : x => [ ] } ; } ( ) ) ; 
var [ , ] = { [ Symbol . iterator ] : function * ( ) { yield * { [ Symbol . iterator ] : async x => 0 } ; } } ;
var [ x ] = { [ Symbol . iterator ] : function * ( ) { yield * { [ Symbol . iterator ] : x => 0 } ; } } ; 
var [ x ] = { [ Symbol . iterator ] : function * ( ) { yield * { [ Symbol . iterator ] : x => [ ] } ; } } ; 
