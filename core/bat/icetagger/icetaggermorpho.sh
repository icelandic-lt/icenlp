lt-proc ../../../../apertium-is-en/is-en.automorf.bin | java -Xmx256M -classpath ../../dist/IceNLPCore.jar is.iclt.icenlp.runner.RunIceTaggerApertium -tm ../../dict/icetagger/otb.apertium.dict -x apertium -sf $1 $2 $3 $4 $5 $6
