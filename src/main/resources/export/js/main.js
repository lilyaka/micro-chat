const IMAGE_FILE_TYPES_SUPPORTED = [
    'apng', 'apng', 'gif', 'jpg', 'jpeg', 'jfif', 'pjpeg', 'pjp', 'png', 'svg', 'webp', 'bmp', 'ico', 'ico'
];

$(document).ready(() => {
    moment.locale('vi');

    document.title = conversationName;
    $("#conversation-name h2").text(conversationName);
    console.log(messages)

    messages.forEach((message, index) => {
        if (index === 0 || moment(message.sentAt).get("date") !== moment(messages[index - 1].sentAt).get("date")) {
            const $messageTime = $($("#message-time").html());
            $messageTime.find("small").text(moment(message.sentAt).format("DD/MM/yyyy"));
            $(".chat-body").append($messageTime);
        }

        if (message.type === "MESSAGE") {
            let messageHtml;
            if (message.fromUserId === userId) {
                messageHtml = $("#right-message-template").html();
            } else {
                messageHtml = $("#left-message-template").html();
            }

            const $message = $(messageHtml);
            $message.attr("id", message.id);
            // $message.find("span.content").text(message.content);
            $message.find("small.time").text(moment(message.sentAt).fromNow());

            $message.prepend(getMessage(message));

            if (message.replyMessage) {
                const $replyMessage = $($("#reply-message-template").html());
                $replyMessage.find("strong").text(message.replyMessage.sender);
                $replyMessage.find("small").text(`"${replyMessageContent(message.replyMessage)}"`);

                $message.prepend($replyMessage);
            }

            $(".chat-body").append($message);
        } else {
            const $messageAction = $($("#message-action").html());
            $messageAction.find("span").text(`${message.sender} ${message.content}`);

            $(".chat-body").append($messageAction);
        }
    });
});

const replyMessageContent = (message) => {
    let content;
    if (message.attachments?.length) {
        content = message.attachments[0].name + (message.attachments.length > 1 ? " ..." : "");
    } else {
        content = message.content;
    }

    return content;
}

const getMessage = (message) => {
    let $messageContent = $($("#text-message-template").html());
    $messageContent.text(message.content);

    if (linkify.find(message.content).length) {
        $messageContent = $($("#link-message-template").html());
        $messageContent.html(linkifyHtml(message.content, {
            target: '_blank'
        }));
    }

    if (message.attachments?.length) {
        if (allAttachmentsIsImage(message.attachments)) {
            $messageContent = $($("#images-message-template").html());
            message.attachments.forEach((image) => {
                const $imageMessage = $($("#image-message-template").html());
                $imageMessage.attr("src", `${downloadUrl}${image.path}`);
                $imageMessage.attr("alt", image.name);
                $messageContent.append($imageMessage);
            });
        } else {
            $messageContent = $($("#files-message-template").html());
            message.attachments.forEach((file) => {
                const $fileMessage = $($("#file-message-template").html());
                $fileMessage.find(".file-icon").attr("src", `./file-extension-icons/${getFileExtension(file.name)}.svg`);
                $fileMessage.find(".file-icon").attr("alt", getFileExtension(file.name));

                $fileMessage.find(".file span").text(file.name);
                $fileMessage.find(".file small").text(convertFileSize(file.size));
                $messageContent.append($fileMessage);
            });
        }
    }

    return $messageContent;
}

const getFileExtension = (fileName, includeDot = false) => {
    return fileName.substring(fileName.lastIndexOf('.') + (includeDot ? 0 : 1), fileName.length);
}

const allAttachmentsIsImage = (attachments) => {
    return attachments.every((attachment) => IMAGE_FILE_TYPES_SUPPORTED.includes(getFileExtension(attachment.name)))
}

const convertFileSize = (size, decimals = 2) => {
    size = typeof size === 'string' ? parseInt(size) : size;
    if (size === 0) {
        return '0 Bytes';
    }
    const k = 1024,
        sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
        i = Math.floor(Math.log(size) / Math.log(k));
    return parseFloat((size / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i];
};