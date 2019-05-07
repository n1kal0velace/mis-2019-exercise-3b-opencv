
# OpenCV + Android assignment

To import opencv library into the android without OpenCV Manager, we used the following tutorial:

** https://android.jlelse.eu/a-beginners-guide-to-setting-up-opencv-android-library-on-android-studio-19794e220f3c **


120258
120542

3b Bonus points:

1. Setting up OpenCV:
We had some problem to setup the OpenCV. To overcome this issue we followed the following tutorial
https://android.jlelse.eu/a-beginners-guide-to-setting-up-opencv-android-library-on-android-studio-19794e220f3

2. Rotating Camera View:
In the beginning we couldn't figure out how to rotate the flipped Camera View.
Thanks to a nice fellow student we solved it:
https://github.com/mmbuw-courses/mis-2019-exercise-3b-opencv/pull/1

3. Memory leak
Because of some perfomance & memory issues we initialized the values in onCameraViewStarted
(check the edited code in MainActivity)
Unfortunately, we couldn't solve it properly but it's a known issue while working with OpenCv for Android


