//
//  ImageResize.m
//  ImageResizer PhoneGap / Cordova Plugin
//
//  Created by Raanan Weber on 02.01.12.
//
//  The software is open source, MIT Licensed.
//  Copyright (c) 2012-2013 webXells GmbH , http://www.webxells.com. All rights reserved.
//
// Using the following Libraries (Big thanks to the developers!)
// Image Scaling : http://iphonedevelopertips.com/graphics/how-to-scale-an-image-using-an-objective-c-category.html . Source is added with respected copyright.
// NSData Base64 : NSData Base64 extension by Dave Winer. http://colloquy.info/project/browser/trunk/NSDataAdditions.h?rev=1576,  Source is added with original copyright.
//

#import "ImageResize.h"
#import "UIImage+Scale.h"
#import "NSData+Base64.h"
#import <ImageIO/ImageIO.h>

@implementation ImageResize

@synthesize callbackID;

- (void) resizeImage:(CDVInvokedUrlCommand*)command {
    NSMutableDictionary *options = [command.arguments objectAtIndex:0];
    
    CGFloat width = [[options objectForKey:@"width"] floatValue];
    CGFloat height = [[options objectForKey:@"height"] floatValue];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];
    NSString *format =  [options objectForKey:@"format"];
    bool storeImage = [[options objectForKey:@"storeImage"] boolValue];
    NSString *filename = [options objectForKey:@"filename"];
    
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
    
    //Load the image
    UIImage *img = [self getImageUsingOptions:options];
    NSDictionary *metaDataDic = [self getExifDataFromImage:img];
    CGSize targetSize = CGSizeMake(width, height);
    
    UIImage* scaledImage = [self imageByScalingNotCroppingForSize:img toSize:targetSize];
    
    //    scaledImage = [img scaleToSize:CGSizeMake(newWidth, newHeight)];
    NSNumber *newWidthObj = [[NSNumber alloc] initWithFloat:scaledImage.size.width];
    NSNumber *newHeightObj = [[NSNumber alloc] initWithFloat:scaledImage.size.height];
    
    CDVPluginResult* pluginResult = nil;
    if (storeImage) {
        
        NSString* fullFilePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, filename, format];
        [options setObject:fullFilePath forKey:@"fullFilePath"];
        [options setObject:metaDataDic forKey:@"meta"];
        bool written = [self writeImage:scaledImage withOptions:options];
        if (written) {
            
            NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:[[NSURL fileURLWithPath:fullFilePath] absoluteString], newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"filePath", @"width", @"height", nil]];
            //            NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:@"encodedString", newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"imageData", @"width", @"height", nil]];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    } else {
        NSData* imageDataObject = nil;
        if ([format isEqualToString:@"jpg"]) {
            imageDataObject = (metaDataDic != NULL) ? [self writeMetadataIntoImageData:UIImageJPEGRepresentation(scaledImage, (quality/100.f)) metadata: [[NSMutableDictionary alloc] initWithDictionary: metaDataDic] ] : UIImageJPEGRepresentation(scaledImage, (quality/100.f));
        } else {
            imageDataObject = UIImagePNGRepresentation(scaledImage);
        }
        
        NSString *encodedString = [imageDataObject base64EncodingWithLineLength:0];
        NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:encodedString, newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"imageData", @"width", @"height", nil]];
        
        if (encodedString != nil) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize
{
    UIImage* sourceImage = anImage;
    UIImage* newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = frameSize.width;
    CGFloat targetHeight = frameSize.height;
    CGFloat scaleFactor = 0.0;
    CGSize scaledSize = frameSize;
    
    if (CGSizeEqualToSize(imageSize, frameSize) == NO) {
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;
        
        // opposite comparison to imageByScalingAndCroppingForSize in order to contain the image within the given bounds
        if (widthFactor == 0.0) {
            scaleFactor = heightFactor;
        } else if (heightFactor == 0.0) {
            scaleFactor = widthFactor;
        } else if (widthFactor > heightFactor) {
            scaleFactor = heightFactor; // scale to fit height
        } else {
            scaleFactor = widthFactor; // scale to fit width
        }
        scaledSize = CGSizeMake(floor(width * scaleFactor), floor(height * scaleFactor));
    }
    
    UIGraphicsBeginImageContext(scaledSize); // this will resize
    
    [sourceImage drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];
    
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }
    
    // pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}

- (UIImage*) getImageUsingOptions:(NSDictionary*)options {
    return [[UIImage alloc ]initWithData:[self getImageDataUsingOption:options]];
}

-(NSData*) getImageDataUsingOption:(NSDictionary*)options {
    NSString *imageData = [options objectForKey:@"data"];
    NSString *imageDataType = [options objectForKey:@"imageDataType"];
    
    if([imageDataType isEqualToString:@"base64Image"]==YES) {
        return [NSData dataWithBase64EncodedString:imageData];
    } else {
        return [NSData dataWithContentsOfURL:[NSURL URLWithString:imageData]];
    }
    return nil;
}


-(NSDictionary*)getExifDataFromImage:(UIImage *)currentImage
{
    
    NSData* pngData =  UIImageJPEGRepresentation(currentImage, 1.0);
    
    CGImageSourceRef mySourceRef = CGImageSourceCreateWithData((CFDataRef)pngData, NULL);
    if (mySourceRef != NULL)
    {
        NSDictionary *myMetadata = (__bridge NSDictionary *)CGImageSourceCopyPropertiesAtIndex(mySourceRef,0,NULL);
        return myMetadata;
        
    }
    return NULL;
    
}

- (void) imageSize:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    
    UIImage * img = [self getImageUsingOptions:options];
    NSNumber *width = [[NSNumber alloc] initWithInt:img.size.width];
    NSNumber *height = [[NSNumber alloc] initWithInt:img.size.height];
    NSDictionary* dic = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:width,height,nil] forKeys:[NSArray arrayWithObjects: @"width", @"height", nil]];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dic];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

// Newly implemented method. saveImage is not working
- (BOOL) writeImage:(UIImage *)img withOptions:(NSDictionary *) options {
    NSString *fullFilePath =  [options objectForKey:@"fullFilePath"];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];
    NSDictionary *meta = [options objectForKey:@"meta"] ;
    
    NSData *data = (meta != NULL)? [self writeMetadataIntoImageData:UIImageJPEGRepresentation(img, quality/100.0f) metadata: [[NSMutableDictionary alloc]initWithDictionary:meta]] : UIImageJPEGRepresentation(img, quality/100.0f) ;
    NSError* err = nil;
    if (![data writeToFile:fullFilePath options:NSAtomicWrite error:&err]) {
        return NO;
    } else {
        return YES;
    }
    
}

//http://stackoverflow.com/questions/9006759/how-to-write-exif-metadata-to-an-image-not-the-camera-roll-just-a-uiimage-or-j
-(NSData *)writeMetadataIntoImageData:(NSData *)imageData metadata:(NSMutableDictionary *)metadata {
    // create an imagesourceref
    CGImageSourceRef source = CGImageSourceCreateWithData((__bridge CFDataRef) imageData, NULL);
    
    // this is the type of image (e.g., public.jpeg)
    CFStringRef UTI = CGImageSourceGetType(source);
    
    // create a new data object and write the new image into it
    NSMutableData *dest_data = [NSMutableData data];
    CGImageDestinationRef destination = CGImageDestinationCreateWithData((__bridge CFMutableDataRef)dest_data, UTI, 1, NULL);
    if (!destination) {
        NSLog(@"Error: Could not create image destination");
    }
    // add the image contained in the image source to the destination, overidding the old metadata with our modified metadata
    CGImageDestinationAddImageFromSource(destination, source, 0, (__bridge CFDictionaryRef) metadata);
    BOOL success = NO;
    success = CGImageDestinationFinalize(destination);
    if (!success) {
        NSLog(@"Error: Could not create data from image destination");
    }
    CFRelease(destination);
    CFRelease(source);
    return dest_data;
}

- (void) storeImage:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    UIImage * img = [self getImageUsingOptions:options];
    [self saveImage:img withOptions:options];
}

- (bool) saveImage:(UIImage *)img withOptions:(NSDictionary *) options {
    NSString *format =  [options objectForKey:@"format"];
    NSString *filename =  [options objectForKey:@"filename"];
    NSString *directory =  [options objectForKey:@"directory"];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];
    bool photoAlbum = [[options objectForKey:@"photoAlbum"] boolValue];
    if (photoAlbum == YES) {
        UIImageWriteToSavedPhotosAlbum(img, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), nil);
        return true;
    } else {
        NSData* imageDataObject = nil;
        if ([format isEqualToString:@"jpg"]) {
            imageDataObject = UIImageJPEGRepresentation(img, (quality/100.f));
        } else {
            imageDataObject = UIImagePNGRepresentation(img);
        }

        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];

        NSMutableString* fullFileName;
        if (![directory isEqualToString:@""]) {
            fullFileName = [NSMutableString stringWithString: directory];
            if (![[NSFileManager defaultManager] fileExistsAtPath:fullFileName]) {
                NSError *error = nil;
                [[NSFileManager defaultManager] createDirectoryAtPath:fullFileName withIntermediateDirectories:NO attributes:nil error:&error];
            }
        } else {
            fullFileName = [NSMutableString stringWithString: documentsDirectory];
        }

        [fullFileName appendString:@"/"];
        [fullFileName appendString:filename];
        NSRange r = [filename rangeOfString:format options:NSCaseInsensitiveSearch];
        if (r.location == NSNotFound) {
            [fullFileName appendString:@"."];
            [fullFileName appendString:format];
        }
        NSError *error = nil;
        bool written = [imageDataObject writeToFile:fullFileName options:NSDataWritingAtomic error:&error];
        if (!written) {
            NSLog(@"Write returned error: %@", [error localizedDescription]);
        }
        return written;
    }
}

- (void) imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {
    NSString *message;
    NSString *title;
    if (!error) {
        title = NSLocalizedString(@"Image Saved", @"");
        message = NSLocalizedString(@"The image was placed in your photo album.", @"");
    }
    else {
        title = NSLocalizedString(@"Error", @"");
        message = [error description];
    }
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                    message:message
                                                   delegate:nil
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
}

@end
