build:
	cd dng && mvn clean install
	cd ui && mvn clean package

prepare_dist:
	# download warp-packer if necessary
	test -f warp-packer || (wget -O warp-packer https://github.com/fintermobilityas/warp/releases/download/v0.4.5/linux-x64.warp-packer && chmod +x ./warp-packer)

	# create distribution folder
	test -d dist || mkdir dist
	cp ui/target/Jeniffer2*with-dependencies.jar dist/jeniffer2.jar
	cp USER_MANUAL.* dist

linux_x64:
	# linux x64
	## create dir if necessary and copy run.sh
	test -d bundle-linux-x64 || mkdir bundle-linux-x64
	cp run-linux.sh bundle-linux-x64
	## copy latest compiled version
	cp ui/target/Jeniffer2*with-dependencies.jar bundle-linux-x64/jeniffer2.jar
	## download jre if not present
	test -d bundle-linux-x64/jre || (cd bundle-linux-x64 && wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_linux_hotspot_17.0.5_8.tar.gz && tar -xvf *.tar.gz && mv *jre jre && rm -f *.tar.gz)
	## remove old compiled version
	rm -f dist/jeniffer2-linux-x64.bin
	## pack new version
	./warp-packer --arch linux-x64 --input_dir bundle-linux-x64 --exec run-linux.sh --output dist/jeniffer2-linux-x64.bin
	## make it executable
	chmod +x dist/jeniffer2-linux-x64.bin

windows_x64:
	# windows x64
	## create dir if necessary and copy run.sh
	test -d bundle-windows-x64 || mkdir bundle-windows-x64
	cp run.cmd bundle-windows-x64
	## copy latest compiled version
	cp ui/target/Jeniffer2*with-dependencies.jar bundle-windows-x64/jeniffer2.jar
	## download jre if not present
	test -d bundle-windows-x64/jre || (cd bundle-windows-x64 && wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_windows_hotspot_17.0.5_8.zip && unzip *.zip && mv *jre jre && rm -f *.zip)
	## remove old compiled version
	rm -f dist/jeniffer2-windows-x64.exe
	## pack new version
	./warp-packer --arch windows-x64 --input_dir bundle-windows-x64 --exec run.cmd --output dist/jeniffer2-windows-x64.exe

macos_x64:
	# macos x64
	## create dir if necessary and copy run.sh
	test -d bundle-macos-x64 || mkdir bundle-macos-x64
	cp run-macos.sh bundle-macos-x64
	## copy latest compiled version
	cp ui/target/Jeniffer2*with-dependencies.jar bundle-macos-x64/jeniffer2.jar
	## download jre if not present
	test -d bundle-macos-x64/jre || (cd bundle-macos-x64 && wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_mac_hotspot_17.0.5_8.tar.gz && tar -xvf *.tar.gz && mv *jre jre && rm -f *.tar.gz)
	## remove old compiled version
	rm -f dist/jeniffer2-mac-x64.bin
	## pack new version
	./warp-packer --arch macos-x64 --input_dir bundle-macos-x64 --exec run-macos.sh --output dist/jeniffer2-mac-x64.bin

macos_aarch64:
	# macos aarch64
	## create dir if necessary and copy run.sh
	test -d bundle-macos-aarch64 || mkdir bundle-macos-aarch64
	cp run-macos-aarch64.sh bundle-macos-aarch64
	## copy latest compiled version
	cp ui/target/Jeniffer2*with-dependencies.jar bundle-macos-aarch64/jeniffer2.jar
	## download jre if not present
	test -d bundle-macos-aarch64/jre || (cd bundle-macos-aarch64 && wget https://cdn.azul.com/zulu/bin/zulu17.42.19-ca-fx-jre17.0.7-macosx_aarch64.tar.gz && tar -xvf *.tar.gz && mv zulu17.42.19-ca-fx-jre17.0.7-macosx_aarch64 jre && rm -f *.tar.gz)
	## remove old compiled version
	rm -f dist/jeniffer2-mac-aarch64.bin
	## pack new version
	## the packer only supports x64 but it will probably run in compatibility mode
	./warp-packer --arch macos-x64 --input_dir bundle-macos-aarch64 --exec run-macos-aarch64.sh --output dist/jeniffer2-mac-aarch64.bin


package: prepare_dist linux_x64 windows_x64 macos_x64 macos_aarch64

linux: prepare_dist linux_x64

windows: prepare_dist windows_x64

macos: prepare_dist macos_aarch64 macos_x64

manual:
	pandoc -f markdown -t latex -V linkcolor:lightblue -o USER_MANUAL.pdf USER_MANUAL.md
	pandoc -f markdown -t latex -V linkcolor:lightblue -o USER_MANUAL.de.pdf USER_MANUAL.de.md
