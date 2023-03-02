class x { static #x = 0 + super . x ; } 
class x { static #x = 0 + typeof super . x ; } 
class x { static #x = 0 + ~ super . x ; } 
class x { static #x = 0 ?. [ super . x ] ; } 
class x { static #x = super . x ( ) ; } 
class x { static #x = super . x ++ ; } 
class x { static #x = super . x -- ; } 
class x { static #x = super [ 0 . #x ] ; }
class x { static #x = super [ { [ 0 . #x ] : 0 } ] ; } 
class x { static #x = super [ { [ 0 ?? 0 ] : 0 . #x ^ x } ] ; } 
class x { static #x = super [ { [ 0 ] : super . x ^ x } ] ; } 
class x { static #x = super [ { [ 0 ^ 0 ] : super . x ^ x } ] ; } 
class x { static #x = super [ { [ Symbol . toPrimitive ] : x => super . x = 0 } ] ; } 
class x { static #x = super [ { [ Symbol . toPrimitive ] : x => super [ 0 ( ) . #x = 0 ] } ] ; } 
class x { static #x = super [ { [ Symbol . toPrimitive ] : x => super [ 0 ] } ] ; } 
class x { static #x = super [ { x : 0 . #x } ] ; }
class x { static #x = super [ { x : super . x } ] ; } 
class x { static { class x extends super . x { ; } } } 
class x { static { throw super . x ; } } 
class x { static { var x = super . x ; } } 
class x { static { var { x = super . x } = 0 ; } } 
class x { static { var { } = super . x ; } } 
