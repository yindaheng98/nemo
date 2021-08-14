# NEMO (MobiCom'20)

This is an official Github repository for the MobiCom paper "NEMO: Enabling Neural-enhanced Video Streaming on Commodity Mobile Devices". This project is built upon Google libvpx, Android Exoplayer, and Qualcomm SNPE and consists of C/C++/Java/Python.   
[[Project homepage]](http://ina.kaist.ac.kr/~nemo/) [[Paper]](https://dl.acm.org/doi/10.1145/3372224.3419185) [[Video]](https://www.youtube.com/watch?v=GPHlAUYCk18&ab_channel=ACMSIGMOBILEONLINE)

If you use our work for research, please cite it.
```
@inproceedings{yeo2020nemo,
  title={NEMO: enabling neural-enhanced video streaming on commodity mobile devices},
  author={Yeo, Hyunho and Chong, Chan Ju and Jung, Youngmok and Ye, Juncheol and Han, Dongsu},
  booktitle={Proceedings of the 26th Annual International Conference on Mobile Computing and Networking},
  pages={1--14},
  year={2020}
}
```
Lastly, NEMO is currently protected under the patent and is retricted to be used for the commercial usage.  
* `BY-NC-SA` – [Attribution-NonCommercial-ShareAlike](https://github.com/idleberg/Creative-Commons-Markdown/blob/master/4.0/by-nc-sa.markdown)

## Project structure
```
./nemo
├── video                  # Python: Video downloader/encoder
├── dnn                    # Python: DNN trainer/converter
├── cache_profile          # Python: Anchor point selector
├── player                 # Java, C/C++: Android video player built upon Exoplayer and the SR-integrated codec
./third_party
├── libvpx                 # C/C++: SR-integrated codec
```

## Prerequisites

* OS: Ubuntu 16.04 or higher versions
* HW: NVIDIA GPU
* Docker: https://docs.docker.com/install/
* NVIDIA docker: https://github.com/NVIDIA/nvidia-docker
* Qualcomm SNPE SDK (v1.40.0): https://developer.qualcomm.com/software/qualcomm-neural-processing-sdk/tools 
  (We cannot provide this due to the Qualcom license policy.)

## Guide
We provide a step-by-step guide with a single video (which content is product review).  
All the folloiwing commands must be executed inside the docker. 

### 1. Setup
* Clone the NEMO docker repository
```
git clone https://github.com/chaos5958/nemo-docker.git
```
* Build the docker image 
```
cd ${HOME}/nemo-docker
./build.sh
```
* Run & Attach to the docker
```
cd ${HOME}/nemo-docker
./run.sh
```
* Clone the NEMO main repository
```
git clone --recurse-submodules https://github.com/kaist-ina/nemo.git ${NEMO_CODE_ROOT}
```
* Download/Setup the Qualcomm SNPE SDK as follow:
```
./nemo
├── third_party
    ├── snpe
        ├── benchmarks
        ├── bin
        ├── include
        ├── lib
        ├── models
        ├── share
        ...
```

### 2. Prepare videos

* Download a Youtube video
```
$NEMO_CODE_ROOT/nemo/tool/script/download_video.sh -c product_review
```

* Encode the video 
```
$NEMO_CODE_ROOT/nemo/tool/script/encode_video.sh -c product_review
```
[Details are described in this file.](nemo/tool/README.md)

### 3. Prepare DNNs

* Train a DNN
```
$NEMO_CODE_ROOT/nemo/dnn/script/train_video.sh -g 0 -c product_review -q high -i 240 -o 1080
```

* (Optional) Convert the TF model to the Qualcomm SNPE dlc
```
$NEMO_CODE_ROOT/nemo/dnn/script/convert_tf_to_snpe.sh -g 0 -c product_review -q high -i 240 -o 1080
```

* (Optional) Test the dlc on Qualcomm devices
```
$NEMO_CODE_ROOT/nemo/dnn/script/test_snpe.sh -g 0 -c product_review -q high -r 240 -s 4 -d [device id]
```
[Details are described in this file.](nemo/dnn/README.md)

### 4. Generate a cache profile 

* Setup: Build the SR-integrated codec (x86_64)
```
$NEMO_CODE_ROOT/nemo/cache_profile/script/setup.sh
```

* Generate the cache profile using the codec
```
$NEMO_CODE_ROOT/nemo/cache_profile/script/select_anchor_points.sh -g 0 -c product_review -q high -i 240 -o 1080 -a nemo
```

* (Optional) Analyze frame dependencies & frame types
```
$NEMO_CODE_ROOT/nemo/cache_profile/script/analyze_video.sh -g 0 -c product_review -q high -i 240 -o 1080 -a nemo
```
[Details are described in this file.](nemo/cache_profile/README.md)

### 5. Compare NEMO vs. baselines
* Setup: Build the SR-integrated codec (arm64-v8)
```
$NEMO_CODE_ROOT/nemo/test/script/setup_local.sh 
```
* Setup: Copy data to mobile devices 
```
$NEMO_CODE_ROOT/nemo/test/script/setup_device.sh -c product_review -q high -r 240 -a nemo_0.5 -d [device id]
```
* Measure the latency
```
$NEMO_CODE_ROOT/nemo/test/script/measure_latency.sh -c product_review -q high -r 240 -a nemo_0.5 -d [device id]
```
* Measure the quality
```
$NEMO_CODE_ROOT/nemo/test/script/measure_quality.sh -g 0 -c product_review -q high -i 240 -o 1080 -a nemo_0.5 
```
[Details are described in this file.](nemo/test/README.md)

### 6. Play NEMO in Android smartphones 
* Setup: Copy data to mobile devices
  * 构建模型并模型转化为SNPE
  * 通过ADB将视频、模型数据和cache profile拷贝到安卓设备上
```
$NEMO_CODE_ROOT/nemo/player/script/setup_device.sh -c product_review -q high -r 240 -a nemo_0.5 -d [device id] -a nemo_0.5_16
```
* Run the NEMO player
1. Enable USB debugging and USB install at mobiles.
2. Open `$NEMO_CODE_ROOT/nemo/player` using Android Studio.
3. Set ndk path in local.properties as `ndk.dir=[ndk folder]/android-ndk-14b`; I used `android-ndk-14b`.
4. Build & run it!
```
App information
* product review: You can choose content.
* 1: it is deprecated.
* high: You can choose DNN quality level.
* Decode-Cache: You can choose Decode, Decode-SR (=per frame super-resolution), Decode-cache (=NEMO).
* None: You can choose total playback time.
* 0.5: You can choose the cache profile, 0.5 represents the quality threshold compared to per-frame super-resolutionl.
```

* 在`nemo/player/Exoplayer`作者修改了ExoPlayer的源码:
  * 在`extensions/vp9/src/main/java/com/google/android/exoplayer2/ext/vp9`作者覆盖了ExoPlayer的vp9解码器插件，因为主要的代码都是C实现然后JNI连进来的，所以这里主要是加了几个变量
  * `extensions/vp9/src/main/jni/vpx_jni.cc`是NEMO解码器的主要JNI代码。
    * ~~这个似乎没有引用外部库？难道是把解码器代码整个放进去了？~~
    * 实际上只是添加了NEMO输入参数和初始化NEMO设置的代码，连调用解码器的过程都没改
    * 作者应该只是魔改了libvpx的实现，所以只需要改设置NEMO的几个函数就好了
    * 作者魔改的libvpx是外部库，作者写的安装过程应该是直接覆盖本地的libvpx里面的vp9编码器，所以连外部引用都不需要改
  * 在`nemo/player/app`基于这个修改后的ExoPlayer做出来的简单App