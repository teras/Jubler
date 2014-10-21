PROJECT=resource_creator

update:${PROJECT}
	./${PROJECT} ../license.rtf >../license_dmg

${PROJECT}:${PROJECT}.m
	clang -arch i386 -arch x86_64 -fobjc-link-runtime -framework AppKit ${PROJECT}.m -o ${PROJECT}

clean:
	rm ${PROJECT}
