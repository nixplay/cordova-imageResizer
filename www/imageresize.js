/**
 * An Image Resizer Plugin for PhoneGap. Updated to fit Cordova 2+
 * The JavaScript based plugin fits both the Android and the iOS native plugins.
 * 
 * The software is open source, MIT licensed.
 * Copyright (C) 2012, webXells GmbH All Rights Reserved.
 * 
 * Raanan Weber, webXells GmbH http://www.webxells.com
 */

var ImageResizer = function() {

};

ImageResizer.IMAGE_DATA_TYPE_BASE64 = "base64Image";
ImageResizer.IMAGE_DATA_TYPE_URL = "urlImage";
ImageResizer.RESIZE_TYPE_FACTOR = "factorResize";
ImageResizer.RESIZE_TYPE_PIXEL = "pixelResize";
ImageResizer.RESIZE_TYPE_FIT_WIDTH = "widthResize";
ImageResizer.FORMAT_JPG = "jpg";
ImageResizer.FORMAT_PNG = "png";
ImageResizer.DEFAULT_RESIZE_QUALITY = 70;
ImageResizer.DEFAULT_STORE_QUALITY = 100;

/**
 * Resize an image
 * @param success success callback, will receive the data sent from the native plugin
 * @param fail error callback, will receive an error string describing what went wrong
 * @param imageData The image data, either base64 or local url
 * @param width width factor / width in pixels
 * @param height height factor / height in pixels
 * @param options extra options -  
 *              format : file format to use (ImageResizer.FORMAT_JPG/ImageResizer.FORMAT_PNG) - defaults to JPG
 *              imageDataType : the data type (IMAGE_DATA_TYPE_BASE64/IMAGE_DATA_TYPE_URL) - defaults to Base64
 *              resizeType : type of the resize (RESIZE_TYPE_FACTOR/RESIZE_TYPE_PIXEL/RESIZE_TYPE_FIT_WIDTH) - defaults to RESIZE_TYPE_PIXEL
 *              quality : INTEGER, compression quality - defaults to DEFAULT_RESIZE_QUALITY
 *					 storeImage : store resized image
 * 				 pixelDensity : adjust image size for pixel density (retina on iOS)
 * 				 directory : directory relative to temporary directory of the app to store image
 * 				 filename : filename of stored resized image
 * 				 photoAlbum : whether to store the image in the photo album (true) or temporary directory of the app (false)
 * @returns JSON Object with the following parameters:
 *              imageData : Base64 of the resized image || OR filename if storeImage = 1
 *              height : height of the resized image
 *              width: width of the resized image
 */
ImageResizer.prototype.resizeImage = function(success, fail, imageData, width,
        height, options) {
    if (!options) {
        options = {}
    }
    var params = {
        data: imageData,
        width: width ? height : 0,
        height: height ? height : 0,
        format: options.format ? options.format : ImageResizer.FORMAT_JPG,
        imageDataType: options.imageType ? options.imageType : ImageResizer.IMAGE_DATA_TYPE_BASE64,
        resizeType: options.resizeType ? options.resizeType : ImageResizer.RESIZE_TYPE_PIXEL,
        quality: options.quality ? options.quality : ImageResizer.DEFAULT_RESIZE_QUALITY,
        storeImage: (typeof options.storeImage !== "undefined") ? options.storeImage : 0,
        pixelDensity: (typeof options.pixelDensity !== "undefined") ? options.pixelDensity : 1,
        directory: options.directory ? options.directory : "",
        filename: options.filename ? options.filename : "",
        photoAlbum: (typeof options.photoAlbum !== "undefined") ? options.photoAlbum : 0
    };

    return cordova.exec(success, fail, "ImageResizePlugin",
            "resizeImage", [params]);
}
/**
 * Get an image width and height
 * @param success success callback, will receive the data sent from the native plugin
 * @param fail error callback, will receive an error string describing what went wrong
 * @param imageData The image data, either base64 or local url
 * @param options extra options -  
 *              imageDataType : the data type (IMAGE_DATA_TYPE_BASE64/IMAGE_DATA_TYPE_URL) - defaults to Base64
 * @returns JSON Object with the following parameters:
 *              height : height of the image
 *              width: width of the image
 */
ImageResizer.prototype.getImageSize = function(success, fail, imageData,
        options) {
    if (!options) {
        options = {};
    }
    var params = {
        data: imageData,
        imageDataType: options.imageType ? options.imageType : ImageResizer.IMAGE_DATA_TYPE_BASE64
    };
    return cordova.exec(success, fail, "ImageResizePlugin",
            "imageSize", [params]);
}

/**
 * Store an image locally
 * @param success success callback, will receive the data sent from the native plugin
 * @param fail error callback, will receive an error string describing what went wrong
 * @param imageData The image data, either base64 or local url
 * @param options extra options -  
 *              format : file format to use (ImageResizer.FORMAT_JPG/ImageResizer.FORMAT_PNG) - defaults to JPG
 *              imageDataType : the data type (IMAGE_DATA_TYPE_BASE64/IMAGE_DATA_TYPE_URL) - defaults to Base64
 *              quality : INTEGER, compression quality - defaults to DEFAULT_RESIZE_QUALITY
 * 				 directory : directory relative to temporary directory of the app to store image
 * 				 filename : filename of stored resized image
 * 				 photoAlbum : whether to store the image in the photo album (true) or temporary directory of the app (false)
 * @returns JSON Object with the following parameters:
 *              url : URL of the file just stored
 */
ImageResizer.prototype.storeImage = function(success, fail, imageData, options) {
    if (!options) {
        options = {}
    }
    var params = {
        data: imageData,
        format: options.format ? options.format : ImageResizer.FORMAT_JPG,
        imageDataType: options.imageType ? options.imageType : ImageResizer.IMAGE_DATA_TYPE_BASE64,
        filename: options.filename,
        directory: options.directory,
        quality: options.quality ? options.quality : ImageResizer.DEFAULT_STORE_QUALITY,
		  photoAlbum: (typeof options.photoAlbum !== "undefined") ? options.photoAlbum : 1
    };

    return cordova.exec(success, fail, "ImageResizePlugin",
            "storeImage", [params]);
}

cordova.addConstructor(function() {
	window.imageResizer = new ImageResizer();

	// backwards compatibility	
	window.plugins = window.plugins || {};
	window.plugins.imageResizer = window.imageResizer;
	console.log("Image Resizer Registered under window.imageResizer");
});